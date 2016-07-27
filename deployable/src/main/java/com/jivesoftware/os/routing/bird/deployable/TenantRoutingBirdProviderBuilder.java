/*
 * Copyright 2013 Jive Software, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jivesoftware.os.routing.bird.deployable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.http.client.HttpClient;
import com.jivesoftware.os.routing.bird.http.client.HttpClientConfig;
import com.jivesoftware.os.routing.bird.http.client.HttpClientException;
import com.jivesoftware.os.routing.bird.http.client.HttpClientFactoryProvider;
import com.jivesoftware.os.routing.bird.http.client.HttpResponse;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptorsProvider;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptorsResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

public class TenantRoutingBirdProviderBuilder {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final String routesHost;
    private final int routesPort;
    private final String routesPath;

    public TenantRoutingBirdProviderBuilder(String routesHost, int routesPort, String routesPath) {
        this.routesHost = routesHost;
        this.routesPort = routesPort;
        this.routesPath = routesPath;
    }

    public ConnectionDescriptorsProvider build() {

        HttpClientConfig httpClientConfig = HttpClientConfig.newBuilder().build();
        final HttpClient httpClient = new HttpClientFactoryProvider()
            .createHttpClientFactory(Collections.singletonList(httpClientConfig))
            .createClient(routesHost, routesPort);

        AtomicLong activeCount = new AtomicLong();
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ConnectionDescriptorsProvider connectionsProvider = (connectionsRequest, expectedReleaseGroup) -> {
            activeCount.incrementAndGet();
            try {
                LOG.debug("Requesting connections:{}", connectionsRequest);

                String postEntity;
                try {
                    postEntity = mapper.writeValueAsString(connectionsRequest);
                } catch (JsonProcessingException e) {
                    LOG.error("Error serializing request parameters object to a string.  Object "
                        + "was " + connectionsRequest + " " + e.getMessage());
                    return null;
                }

                HttpResponse response;
                try {
                    response = httpClient.postJson(routesPath, postEntity, null);
                } catch (HttpClientException e) {
                    LOG.error("Error posting query request to server.  The entity posted was {} and the endpoint posted to was {}",
                        new Object[] { postEntity, routesPath }, e);
                    return null;
                }

                int statusCode = response.getStatusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    byte[] responseBody = response.getResponseBody();
                    try {
                        ConnectionDescriptorsResponse connectionDescriptorsResponse = mapper.readValue(responseBody, ConnectionDescriptorsResponse.class);
                        if (!connectionsRequest.getRequestUuid().equals(connectionDescriptorsResponse.getRequestUuid())) {
                            LOG.warn("Request UUIDs are misaligned, request:{} response:{}", connectionsRequest, connectionDescriptorsResponse);
                        }
                        if (expectedReleaseGroup != null && !expectedReleaseGroup.equals(connectionDescriptorsResponse.getReleaseGroup())) {
                            String responseEntity = new String(responseBody, StandardCharsets.UTF_8);
                            LOG.warn("Release group changed, active:{} request:{} requestEntity:{} responseEntity:{} response:{}",
                                activeCount.get(), connectionsRequest, postEntity, responseEntity, connectionDescriptorsResponse);
                        }
                        LOG.debug("Request:{} ConnectionDescriptors:{}", connectionsRequest, connectionDescriptorsResponse);
                        return connectionDescriptorsResponse;
                    } catch (IOException x) {
                        LOG.error("Failed to deserialize response:" + new String(responseBody) + " " + x.getMessage());
                        return null;
                    }
                }
                return null;
            } finally {
                activeCount.decrementAndGet();
            }
        };
        return connectionsProvider;
    }
}
