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
import com.jivesoftware.os.routing.bird.shared.HttpClientException;
import java.util.Map;

public class LatentHttpClient implements HttpClient {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final HttpClient delegate;

    public LatentHttpClient(HttpClient delegate) {
        this.delegate = delegate;
    }

    private void randSleep() {
        final long maxSleepMs = 10_000L;

        try {
            long sleepMs = (long) (Math.random() * maxSleepMs);
            LOG.debug("Sleep induced latency {}ms", sleepMs);
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public HttpResponse get(String path, Map<String, String> headers)
        throws HttpClientException {
        randSleep();
        return delegate.get(path, headers);
    }

    @Override
    public HttpResponse delete(String path, Map<String, String> headers)
        throws HttpClientException {
        randSleep();
        return delegate.delete(path, headers);
    }

    @Override
    public HttpResponse postBytes(String path, byte[] postBytes, Map<String, String> headers)
        throws HttpClientException {
        randSleep();
        return delegate.postBytes(path, postBytes, headers);
    }

    @Override
    public HttpResponse postJson(String path, String postJsonBody, Map<String, String> headers)
        throws HttpClientException {
        randSleep();
        return delegate.postJson(path, postJsonBody, headers);
    }

    @Override
    public HttpStreamResponse streamingPost(String path, String postJsonBody, Map<String, String> headers)
        throws HttpClientException {
        randSleep();
        return delegate.streamingPost(path, postJsonBody, headers);
    }

    @Override
    public HttpStreamResponse streamingPostStreamableRequest(String path,
        HttpClient.StreamableRequest streamableRequest,
        Map<String, String> headers) throws HttpClientException {
        randSleep();
        return delegate.streamingPostStreamableRequest(path, streamableRequest, headers);
    }

    @Override
    public HttpResponse postStreamableRequest(String path,
        HttpClient.StreamableRequest streamableRequest,
        Map<String, String> headers) throws HttpClientException {
        randSleep();
        return delegate.postStreamableRequest(path, streamableRequest, headers);
    }

    @Override
    public void close() {
        delegate.close();
    }

}
