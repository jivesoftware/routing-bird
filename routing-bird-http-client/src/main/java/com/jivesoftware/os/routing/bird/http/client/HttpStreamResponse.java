package com.jivesoftware.os.routing.bird.http.client;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.util.EntityUtils;

public class HttpStreamResponse {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    protected final int statusCode;
    protected final String statusReasonPhrase;
    protected final CloseableHttpResponse closeableHttpResponse;
    protected final InputStream inputStream;
    protected final HttpRequestBase requestBase;
    protected final AtomicLong activeCount;

    public HttpStreamResponse(int statusCode,
        String statusReasonPhrase,
        CloseableHttpResponse closeableHttpResponse,
        InputStream inputStream,
        HttpRequestBase requestBase,
        AtomicLong activeCount) {
        this.statusCode = statusCode;
        this.statusReasonPhrase = statusReasonPhrase;
        this.closeableHttpResponse = closeableHttpResponse;
        this.inputStream = inputStream;
        this.requestBase = requestBase;
        this.activeCount = activeCount;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusReasonPhrase() {
        return statusReasonPhrase;
    }

    public void close() {
        HttpClientUtils.closeQuietly(closeableHttpResponse);
        requestBase.reset();
        activeCount.decrementAndGet();
    }

    public long getActiveCount() {
        return activeCount.get();
    }
}
