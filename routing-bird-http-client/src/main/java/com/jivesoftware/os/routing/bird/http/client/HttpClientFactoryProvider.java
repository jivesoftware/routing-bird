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
package com.jivesoftware.os.routing.bird.http.client;

import java.io.Closeable;
import java.util.Collection;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;

public class HttpClientFactoryProvider {

    public HttpClientFactory createHttpClientFactory(final Collection<HttpClientConfiguration> configurations) {
        return createHttpClientFactory(configurations, -1, -1, false);
    }

    public HttpClientFactory createHttpClientFactory(final Collection<HttpClientConfiguration> configurations,
        long debugClientCount,
        long debugClientCountInterval,
        boolean latentClient) {

        final HttpClientConfig httpClientConfig = locateConfig(configurations, HttpClientConfig.class, HttpClientConfig.newBuilder().build());
        final PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();

        if (httpClientConfig.getMaxConnections() > 0) {
            poolingHttpClientConnectionManager.setMaxTotal(httpClientConfig.getMaxConnections());
        } else {
            poolingHttpClientConnectionManager.setMaxTotal(Integer.MAX_VALUE);
        }

        if (httpClientConfig.getMaxConnectionsPerHost() > 0) {
            poolingHttpClientConnectionManager.setDefaultMaxPerRoute(httpClientConfig.getMaxConnectionsPerHost());
        } else {
            poolingHttpClientConnectionManager.setDefaultMaxPerRoute(Integer.MAX_VALUE);
        }

        HttpClientSSLConfig sslConfig = locateConfig(configurations, HttpClientSSLConfig.class, null);
        String scheme;
        if (sslConfig != null && sslConfig.isUseSsl()) {
            scheme = "https";
        } else {
            scheme = "http";
        }

        poolingHttpClientConnectionManager
            .setDefaultSocketConfig(SocketConfig.custom()
                .setSoTimeout(httpClientConfig.getSocketTimeoutInMillis() > 0 ? httpClientConfig.getSocketTimeoutInMillis() : 0)
                .build());

        Closeable closeable;
        HttpClientConnectionManager clientConnectionManager;
        if (debugClientCountInterval >= 0) {
            LeakDetectingHttpClientConnectionManager leakDetectingHttpClientConnectionManager = new LeakDetectingHttpClientConnectionManager(
                poolingHttpClientConnectionManager, debugClientCount, debugClientCountInterval);
            clientConnectionManager = leakDetectingHttpClientConnectionManager;
            closeable = leakDetectingHttpClientConnectionManager::close;
        } else {
            clientConnectionManager = poolingHttpClientConnectionManager;
            closeable = poolingHttpClientConnectionManager;
        }

        return (String host, int port) -> {
            HttpRoutePlanner rp = new DefaultRoutePlanner(DefaultSchemePortResolver.INSTANCE) {
                @Override
                public HttpRoute determineRoute(
                    final HttpHost httpHost,
                    final HttpRequest request,
                    final HttpContext context) throws HttpException {
                    HttpHost target = httpHost != null ? httpHost : new HttpHost(host, port, scheme);
                    return super.determineRoute(target, request, context);
                }
            };

            HttpClientBuilder httpClientBuilder = HttpClients.custom()
                .setConnectionManager(clientConnectionManager)
                .setRoutePlanner(rp);

            if (sslConfig != null && sslConfig.isUseSsl()) {
                if (sslConfig.getCustomSSLSocketFactory() != null) {
                    httpClientBuilder.setSSLSocketFactory(sslConfig.getCustomSSLSocketFactory());
                }
            }

            CloseableHttpClient client = httpClientBuilder.build();

            if (latentClient) {
                return new LatentHttpClient(client,
                    closeable,
                    httpClientConfig.getCopyOfHeadersForEveryRequest());
            }

            return new ApacheHttpClient441BackedHttpClient(client,
                closeable,
                httpClientConfig.getCopyOfHeadersForEveryRequest());
        };
    }

    @SuppressWarnings("unchecked")
    private <T> T locateConfig(Collection<HttpClientConfiguration> configurations, Class<? extends T> _class, T defaultConfiguration) {
        for (HttpClientConfiguration configuration : configurations) {
            if (_class.isInstance(configuration)) {
                return (T) configuration;
            }
        }
        return defaultConfiguration;
    }

}
