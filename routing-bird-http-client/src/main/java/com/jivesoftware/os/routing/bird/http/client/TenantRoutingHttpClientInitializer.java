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

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.http.client.ClientHealthProvider.ClientHealth;
import com.jivesoftware.os.routing.bird.shared.ClientConnectionsFactory;
import com.jivesoftware.os.routing.bird.shared.ClientsCloser;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptor;
import com.jivesoftware.os.routing.bird.shared.TenantRoutingClient;
import com.jivesoftware.os.routing.bird.shared.TenantsServiceConnectionDescriptorProvider;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLContext;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContexts;

public class TenantRoutingHttpClientInitializer<T> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    public Builder<T> builder(
        TenantsServiceConnectionDescriptorProvider<T> connectionPoolProvider,
        ClientHealthProvider clientHealthProvider) {
        return new Builder<>(connectionPoolProvider, clientHealthProvider);
    }

    public static class Builder<T> {

        private final TenantsServiceConnectionDescriptorProvider<T> connectionPoolProvider;
        private final ClientHealthProvider clientHealthProvider;

        private int deadAfterNErrors = 10;
        private long checkDeadEveryNMillis = 10_000;

        private int maxConnections = 32;
        private int maxConnectionsPerHost = -1;

        private int socketTimeoutInMillis = 600_000;

        private long debugClientCount = -1;
        private long debugClientCountInterval = -1;

        private String instanceKey;
        private String privateKey;

        private Builder(TenantsServiceConnectionDescriptorProvider<T> connectionPoolProvider,
            ClientHealthProvider clientHealthProvider) {
            this.connectionPoolProvider = connectionPoolProvider;
            this.clientHealthProvider = clientHealthProvider;
        }

        public Builder<T> deadAfterNErrors(int deadAfterNErrors) {
            this.deadAfterNErrors = deadAfterNErrors;
            return this;
        }

        public Builder<T> checkDeadEveryNMillis(long checkDeadEveryNMillis) {
            this.checkDeadEveryNMillis = checkDeadEveryNMillis;
            return this;
        }

        public Builder<T> maxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }

        public Builder<T> maxConnectionsPerHost(int maxConnectionsPerHost) {
            this.maxConnectionsPerHost = maxConnectionsPerHost;
            return this;
        }

        public Builder<T> socketTimeoutInMillis(int socketTimeoutInMillis) {
            this.socketTimeoutInMillis = socketTimeoutInMillis;
            return this;
        }

        public Builder<T> debugClient(long debugClientCount, long debugClientCountInterval) {
            this.debugClientCount = debugClientCount;
            this.debugClientCountInterval = debugClientCountInterval;
            return this;
        }

        public Builder<T> setInstanceKey(String instanceKey) {
            this.instanceKey = instanceKey;
            return this;
        }

        public Builder<T> setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
            return this;
        }

        public TenantAwareHttpClient<T> build() {
            ClientConnectionsFactory<HttpClient, HttpClientException> clientConnectionsFactory = (routingGroup, connectionDescriptors) -> {
                List<ConnectionDescriptor> descriptors = connectionDescriptors.getConnectionDescriptors();
                ConnectionDescriptor[] connections = descriptors.toArray(new ConnectionDescriptor[descriptors.size()]);
                HttpClient[] httpClients = new HttpClient[descriptors.size()];
                ClientHealth[] clientHealths = new ClientHealth[descriptors.size()];
                HttpClientFactoryProvider httpClientFactoryProvider = new HttpClientFactoryProvider();
                for (int i = 0; i < connections.length; i++) {
                    ConnectionDescriptor connection = connections[i];

                    List<HttpClientConfiguration> config = new ArrayList<>();
                    config.add(HttpClientConfig
                        .newBuilder()
                        .setMaxConnections(maxConnections)
                        .setMaxConnectionsPerHost(maxConnectionsPerHost)
                        .setSocketTimeoutInMillis(socketTimeoutInMillis)
                        .build());

                    OAuthSigner signer = null;
                    if (connection.getSslEnabled()) {
                        HttpClientSSLConfig.Builder builder = HttpClientSSLConfig.newBuilder();
                        builder.setUseSSL(true);
                        if (true) {
                            LOG.warn("Need to fix ALWAYS allowing selft sigend cert!");
                            SSLContext sslcontext = SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
                            // Allow TLSv1 protocol only, use NoopHostnameVerifier to trust self-singed cert
                            builder.setUseSslWithCustomSSLSocketFactory(new SSLConnectionSocketFactory(sslcontext,
                                new String[]{"TLSv1"}, null, new NoopHostnameVerifier()));

                        }
                        config.add(builder.build());

                    }

                    if (connection.getServiceAuthEnabled()) {
                        String consumerKey = instanceKey;
                        String consumerSecret = privateKey; // RSA my private key
                        String token = consumerKey;
                        String tokenSecret = consumerSecret;

                        CommonsHttpOAuthConsumer oAuthConsumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);
                        oAuthConsumer.setTokenWithSecret(token, tokenSecret);
                        signer = ((request) -> {
                            return oAuthConsumer.sign(request);
                        });
                    }
                    boolean latentClient = connection.getMonkeys() != null && connection.getMonkeys().containsKey("RANDOM_CONNECTION_LATENCY");

                    HttpClientFactory createHttpClientFactory = httpClientFactoryProvider.createHttpClientFactory(config, latentClient);
                    httpClients[i] = createHttpClientFactory.createClient(
                        signer,
                        connection.getHostPort().getHost(),
                        connection.getHostPort().getPort());

                    clientHealths[i] = clientHealthProvider.get(connection);
                }

                return new ErrorCheckingTimestampedClients<>(
                    routingGroup,
                    connectionDescriptors.getTimestamp(),
                    connections,
                    httpClients,
                    clientHealths,
                    deadAfterNErrors,
                    checkDeadEveryNMillis);
            };

            ClientsCloser<HttpClient> clientsCloser = clients -> {
                for (HttpClient client : clients) {
                    client.close();
                }
            };

            TenantRoutingClient<T, HttpClient, HttpClientException> tenantRoutingClient = new TenantRoutingClient<>(connectionPoolProvider,
                clientConnectionsFactory, clientsCloser);
            return new TenantRoutingHttpClient<>(tenantRoutingClient);
        }
    }
}
