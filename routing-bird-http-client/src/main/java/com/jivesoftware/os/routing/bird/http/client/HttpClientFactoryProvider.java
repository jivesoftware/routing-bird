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
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.*;
import org.apache.http.HttpResponse;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;

public class HttpClientFactoryProvider {

    public HttpClientFactory createHttpClientFactory(final Collection<HttpClientConfiguration> configurations) {
        return createHttpClientFactory(configurations, -1, -1);
    }

    public HttpClientFactory createHttpClientFactory(final Collection<HttpClientConfiguration> configurations,
        long debugClientCount,
        long debugClientCountInterval) {

        final HttpClientConfig httpClientConfig = locateConfig(configurations, HttpClientConfig.class, HttpClientConfig.newBuilder().build());
        final PoolingHttpClientConnectionManager clientConnectionManager = new PoolingHttpClientConnectionManager();

        if (httpClientConfig.getMaxConnections() > 0) {
            clientConnectionManager.setMaxTotal(httpClientConfig.getMaxConnections());
        } else {
            clientConnectionManager.setMaxTotal(Integer.MAX_VALUE);
        }

        if (httpClientConfig.getMaxConnectionsPerHost() > 0) {
            clientConnectionManager.setDefaultMaxPerRoute(httpClientConfig.getMaxConnectionsPerHost());
        } else {
            clientConnectionManager.setDefaultMaxPerRoute(Integer.MAX_VALUE);
        }

        clientConnectionManager.setDefaultSocketConfig(SocketConfig.custom()
            .setSoTimeout(httpClientConfig.getSocketTimeoutInMillis() > 0 ? httpClientConfig.getSocketTimeoutInMillis() : 0)
            .build());

        LeakDetectingHttpClientConnectionManager leakDetectingHttpClientConnectionManager = new LeakDetectingHttpClientConnectionManager(
            clientConnectionManager, debugClientCount, debugClientCountInterval);

        return new HttpClientFactory() {
            @Override
            public HttpClient createClient(final String host, final int port) {
                HttpRoutePlanner rp = new DefaultRoutePlanner(DefaultSchemePortResolver.INSTANCE) {

                    @Override
                    public HttpRoute determineRoute(
                        final HttpHost httpHost,
                        final HttpRequest request,
                        final HttpContext context) throws HttpException {
                        HttpHost target = httpHost != null ? httpHost : new HttpHost(host, port);
                        return super.determineRoute(target, request, context);
                    }
                };
                CloseableHttpClient client = HttpClients.custom()
                    .setConnectionManager(leakDetectingHttpClientConnectionManager)
                    .setRoutePlanner(rp)
                    .build();

                return new ApacheHttpClient441BackedHttpClient(client,
                    leakDetectingHttpClientConnectionManager::close,
                    httpClientConfig.getCopyOfHeadersForEveryRequest());
            }
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

    public static class LeakDetectingHttpClientConnectionManager implements HttpClientConnectionManager {

        private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

        private static final AtomicLong LEAK_DETECTOR_IDS = new AtomicLong(0);

        private final PoolingHttpClientConnectionManager delegate;
        private final long debugClientCount;
        private final long debugClientCountInterval;

        private final long leakDetectorId = LEAK_DETECTOR_IDS.incrementAndGet();
        private final AtomicLong connectionIds = new AtomicLong(0);
        private final AtomicLong activeCount = new AtomicLong(0);
        private final Map<Long, String> leased = new ConcurrentHashMap<>();
        private volatile long lastDebugClientTime = 0;

        public LeakDetectingHttpClientConnectionManager(PoolingHttpClientConnectionManager delegate,
            long debugClientCount,
            long debugClientCountInterval) {
            this.delegate = delegate;
            this.debugClientCount = debugClientCount;
            this.debugClientCountInterval = debugClientCountInterval;
        }

        private void debug() {
            long ctm = System.currentTimeMillis();
            if (debugClientCountInterval >= 0) {
                long count = activeCount.get();
                if (count >= debugClientCount) {
                    if (ctm - lastDebugClientTime >= debugClientCountInterval) {
                        LOG.info("Manager {} active client count: {} leased: {}", leakDetectorId, count, leased);
                        lastDebugClientTime = ctm;
                    }
                }
            }
        }

        @Override
        public ConnectionRequest requestConnection(HttpRoute route, Object state) {
            ConnectionRequest connectionRequest = delegate.requestConnection(route, state);
            String threadName = Thread.currentThread().getName();
            return new ConnectionRequest() {
                @Override
                public HttpClientConnection get(long timeout, TimeUnit tunit) throws InterruptedException, ExecutionException, ConnectionPoolTimeoutException {
                    activeCount.incrementAndGet();
                    debug();
                    HttpClientConnection httpClientConnection = connectionRequest.get(timeout, tunit);
                    long connectionId = connectionIds.incrementAndGet();
                    if (debugClientCountInterval >= 0) {
                        String stackTrace = threadName + ": " + ExceptionUtils.getStackTrace(new Throwable());
                        leased.put(connectionId, stackTrace);
                    }
                    return new LeakDetectingHttpClientConnection(httpClientConnection, connectionId);
                }

                @Override
                public boolean cancel() {
                    return connectionRequest.cancel();
                }
            };
        }

        @Override
        public void releaseConnection(HttpClientConnection conn, Object newState, long validDuration, TimeUnit timeUnit) {
            activeCount.decrementAndGet();
            if (debugClientCountInterval >= 0) {
                if (conn instanceof LeakDetectingHttpClientConnection) {
                    long connectionId = ((LeakDetectingHttpClientConnection) conn).getId();
                    leased.remove(connectionId);
                } else {
                    LOG.warn("Released connection was not a leak detector");
                }
            }
            delegate.releaseConnection(conn, newState, validDuration, timeUnit);
        }

        @Override
        public void connect(HttpClientConnection conn, HttpRoute route, int connectTimeout, HttpContext context) throws IOException {
            delegate.connect(conn, route, connectTimeout, context);
        }

        @Override
        public void upgrade(HttpClientConnection conn, HttpRoute route, HttpContext context) throws IOException {
            delegate.upgrade(conn, route, context);
        }

        @Override
        public void routeComplete(HttpClientConnection conn, HttpRoute route, HttpContext context) throws IOException {
            delegate.routeComplete(conn, route, context);
        }

        @Override
        public void closeIdleConnections(long idletime, TimeUnit tunit) {
            delegate.closeIdleConnections(idletime, tunit);
        }

        @Override
        public void closeExpiredConnections() {
            delegate.closeExpiredConnections();
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        public void close() {
            delegate.close();
        }
    }

    public static class LeakDetectingHttpClientConnection implements HttpClientConnection {

        private final HttpClientConnection delegate;
        private final long id;

        public LeakDetectingHttpClientConnection(HttpClientConnection delegate, long id) {
            this.delegate = delegate;
            this.id = id;
        }

        public long getId() {
            return id;
        }

        @Override
        public boolean isResponseAvailable(int timeout) throws IOException {
            return delegate.isResponseAvailable(timeout);
        }

        @Override
        public void sendRequestHeader(HttpRequest request) throws HttpException, IOException {
            delegate.sendRequestHeader(request);
        }

        @Override
        public void sendRequestEntity(HttpEntityEnclosingRequest request) throws HttpException, IOException {
            delegate.sendRequestEntity(request);
        }

        @Override
        public HttpResponse receiveResponseHeader() throws HttpException, IOException {
            return delegate.receiveResponseHeader();
        }

        @Override
        public void receiveResponseEntity(HttpResponse response) throws HttpException, IOException {
            delegate.receiveResponseEntity(response);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }

        @Override
        public boolean isOpen() {
            return delegate.isOpen();
        }

        @Override
        public boolean isStale() {
            return delegate.isStale();
        }

        @Override
        public void setSocketTimeout(int timeout) {
            delegate.setSocketTimeout(timeout);
        }

        @Override
        public int getSocketTimeout() {
            return delegate.getSocketTimeout();
        }

        @Override
        public void shutdown() throws IOException {
            delegate.shutdown();
        }

        @Override
        public HttpConnectionMetrics getMetrics() {
            return delegate.getMetrics();
        }
    }
}
