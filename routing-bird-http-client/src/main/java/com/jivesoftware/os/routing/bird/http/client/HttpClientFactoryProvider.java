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
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;

public class HttpClientFactoryProvider {

    public HttpClientFactory createHttpClientFactory(Collection<HttpClientConfiguration> configurations, boolean latentClient) {

        HttpClientConfig httpClientConfig = locateConfig(configurations, HttpClientConfig.class, HttpClientConfig.newBuilder().build());
        HttpClientSSLConfig sslConfig = locateConfig(configurations, HttpClientSSLConfig.class, null);

        String scheme;
        PoolingHttpClientConnectionManager poolingHttpClientConnectionManager;
        if (sslConfig != null && sslConfig.isUseSsl()) {
            LayeredConnectionSocketFactory sslSocketFactory;
            if (sslConfig.getCustomSSLSocketFactory() != null) {
                sslSocketFactory = sslConfig.getCustomSSLSocketFactory();
            } else {
                sslSocketFactory = SSLConnectionSocketFactory.getSocketFactory();
            }

            scheme = "https";
            poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslSocketFactory)
                .build());
        } else {
            scheme = "http";
            poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
        }

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

        poolingHttpClientConnectionManager
            .setDefaultSocketConfig(SocketConfig.custom()
                .setSoTimeout(httpClientConfig.getSocketTimeoutInMillis() > 0 ? httpClientConfig.getSocketTimeoutInMillis() : 0)
                .build());

        Closeable closeable;
        HttpClientConnectionManager clientConnectionManager;
        clientConnectionManager = poolingHttpClientConnectionManager;
        closeable = poolingHttpClientConnectionManager;

        return (OAuthSigner signer, String host, int port) -> {
            HttpClientBuilder httpClientBuilder = HttpClients.custom()
                .setConnectionManager(clientConnectionManager);

            CloseableHttpClient client = httpClientBuilder.build();
            HttpClient httpClient = new ApacheHttpClient441BackedHttpClient(scheme,
                host,
                port,
                signer,
                client,
                closeable,
                httpClientConfig.getCopyOfHeadersForEveryRequest());

            if (latentClient) {
                httpClient = new LatentHttpClient(httpClient);
            }
            return httpClient;
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
