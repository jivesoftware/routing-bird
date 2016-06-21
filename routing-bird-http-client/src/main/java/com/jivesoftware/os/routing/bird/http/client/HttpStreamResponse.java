package com.jivesoftware.os.routing.bird.http.client;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;

public class HttpStreamResponse {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    protected final int statusCode;
    protected final String statusReasonPhrase;
    protected final HttpEntity httpEntity;
    protected final InputStream inputStream;
    protected final HttpRequestBase requestBase;
    protected final AtomicLong activeCount;

    public HttpStreamResponse(int statusCode, String statusReasonPhrase, HttpEntity httpEntity, InputStream inputStream, HttpRequestBase requestBase, AtomicLong activeCount) {
        this.statusCode = statusCode;
        this.statusReasonPhrase = statusReasonPhrase;
        this.httpEntity = httpEntity;
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
        try {
            EntityUtils.consume(httpEntity);
        } catch (IOException e) {
            LOG.error("Failed to consume streaming entity", e);
        }
        requestBase.reset();
        activeCount.decrementAndGet();
    }

    public long getActiveCount() {
        return activeCount.get();
    }
}
