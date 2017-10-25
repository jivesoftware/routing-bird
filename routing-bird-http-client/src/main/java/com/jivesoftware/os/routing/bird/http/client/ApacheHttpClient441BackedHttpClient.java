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
import com.jivesoftware.os.routing.bird.shared.HttpClientPoolStats;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.pool.ConnPoolControl;
import org.apache.http.pool.PoolStats;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

class ApacheHttpClient441BackedHttpClient implements HttpClient {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger(true);

    private static final int JSON_POST_LOG_LENGTH_LIMIT = 2048;
    private static final String TIMER_NAME = "OutboundHttpRequest";

    private final String scheme;
    private final String host;
    private final int port;
    private final OAuthSigner oauthSigner;
    private final CloseableHttpClient client;
    private final ConnPoolControl<HttpRoute> connPoolControl;
    private final Closeable onClose;
    private final Map<String, String> headersForEveryRequest;
    private final AtomicLong activeCount = new AtomicLong(0);

    public ApacheHttpClient441BackedHttpClient(String scheme,
        String host,
        int port,
        OAuthSigner signer,
        CloseableHttpClient client,
        Closeable onClose,
        ConnPoolControl<HttpRoute> connPoolControl,
        Map<String, String> headersForEveryRequest) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;

        this.oauthSigner = signer;
        this.client = client;
        this.onClose = onClose;
        this.connPoolControl = connPoolControl;
        this.headersForEveryRequest = headersForEveryRequest;
    }

    @Override
    public HttpClientPoolStats getPoolStats() {
        PoolStats totalStats = connPoolControl.getTotalStats();
        return new HttpClientPoolStats(totalStats.getLeased(),
            totalStats.getPending(),
            totalStats.getAvailable(),
            totalStats.getMax());
    }

    @Override
    public void close() {
        try {
            HttpClientUtils.closeQuietly(client);
            onClose.close();
        } catch (IOException t) {
            LOG.error("Failed to close client", t);
        }
    }

    private URI toURI(String path) throws HttpClientException {
        try {
            return new URI(scheme + "://" + host + ':' + port + (path.startsWith("/") ? path : '/' + path));
        } catch (URISyntaxException e) {
            throw new HttpClientException("Bad URI", e);
        }
    }

    @Override
    public HttpStreamResponse streamingPost(String path, String postJsonBody, Map<String, String> headers) throws HttpClientException {
        return executePostJsonStreamingResponse(new HttpPost(toURI(path)), postJsonBody, headers);
    }

    private String clientToString() {
        return scheme + "://" + host + ':' + port;
    }

    private HttpStreamResponse executePostJsonStreamingResponse(HttpEntityEnclosingRequestBase requestBase,
        String jsonBody,
        Map<String, String> headers) throws HttpClientException {
        try {
            setRequestHeaders(headers, requestBase);
            requestBase.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
            requestBase.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            return executeStream(requestBase);
        } catch (IOException | UnsupportedCharsetException | OAuthCommunicationException | OAuthExpectationFailedException |
            OAuthMessageSignerException e) {
            String trimmedMethodBody = (jsonBody.length() > JSON_POST_LOG_LENGTH_LIMIT)
                ? jsonBody.substring(0, JSON_POST_LOG_LENGTH_LIMIT) : jsonBody;
            throw new HttpClientException("Error executing " + requestBase.getMethod() + " request " +
                "to:" + clientToString() + "/" + requestBase.getURI().getPath() + " body:" + trimmedMethodBody, e);
        }
    }

    private HttpStreamResponse executeStream(HttpRequestBase requestBase) throws OAuthMessageSignerException,
        OAuthExpectationFailedException,
        OAuthCommunicationException,
        IOException {
        applyHeadersCommonToAllRequests(requestBase);
        activeCount.incrementAndGet();
        CloseableHttpResponse response = client.execute(requestBase);
        StatusLine statusLine = response.getStatusLine();

        int status = statusLine.getStatusCode();
        if (status < 200 || status >= 300) {
            activeCount.decrementAndGet();
            HttpClientUtils.closeQuietly(response);
            requestBase.reset();
            throw new IOException("Bad status : " + statusLine);
        }

        return new HttpStreamResponse(statusLine.getStatusCode(),
            statusLine.getReasonPhrase(),
            response,
            response.getEntity().getContent(),
            requestBase,
            activeCount);
    }

    @Override
    public HttpResponse get(String path, Map<String, String> headers) throws HttpClientException {
        HttpGet get = new HttpGet(toURI(path));
        setRequestHeaders(headers, get);
        try {
            return execute(get);
        } catch (IOException | OAuthCommunicationException | OAuthExpectationFailedException | OAuthMessageSignerException e) {
            throw new HttpClientException("Error executing GET request " +
                "to:" + clientToString() + "/" + path, e);
        }
    }

    @Override
    public HttpResponse delete(String path, Map<String, String> headers) throws HttpClientException {
        HttpDelete delete = new HttpDelete(toURI(path));
        setRequestHeaders(headers, delete);
        try {
            return execute(delete);
        } catch (IOException | OAuthCommunicationException | OAuthExpectationFailedException | OAuthMessageSignerException e) {
            throw new HttpClientException("Error executing GET request " +
                "to:" + clientToString() + "/" + path, e);
        }
    }

    @Override
    public HttpResponse postJson(String path, String postJsonBody, Map<String, String> headers) throws HttpClientException {
        try {
            HttpPost post = new HttpPost(toURI(path));
            setRequestHeaders(headers, post);
            post.setEntity(new StringEntity(postJsonBody, ContentType.APPLICATION_JSON));
            post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            return execute(post);
        } catch (IOException | UnsupportedCharsetException | OAuthCommunicationException | OAuthExpectationFailedException | OAuthMessageSignerException e) {
            String trimmedPostBody = (postJsonBody.length() > JSON_POST_LOG_LENGTH_LIMIT)
                ? postJsonBody.substring(0, JSON_POST_LOG_LENGTH_LIMIT) : postJsonBody;
            throw new HttpClientException("Error executing POST json request " +
                "to:" + clientToString() + "/" + path + " body:" + trimmedPostBody, e);
        }
    }

    @Override
    public HttpResponse postBytes(String path, byte[] postBytes, Map<String, String> headers) throws HttpClientException {
        try {
            HttpPost post = new HttpPost(toURI(path));
            setRequestHeaders(headers, post);
            post.setEntity(new ByteArrayEntity(postBytes, ContentType.APPLICATION_OCTET_STREAM));
            post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM);
            return execute(post);
        } catch (IOException | OAuthCommunicationException | OAuthExpectationFailedException | OAuthMessageSignerException e) {
            throw new HttpClientException("Error executing POST bytes request " +
                "to:" + clientToString() + "/" + path + " byte length:" + postBytes.length, e);
        }
    }

    @Override
    public HttpStreamResponse streamingPostStreamableRequest(String path,
        StreamableRequest streamable,
        Map<String, String> headers) throws HttpClientException {
        try {
            HttpPost post = new HttpPost(toURI(path));
            setRequestHeaders(headers, post);
            post.setEntity(new StreamableEntity(streamable));
            post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM);
            return executeStream(post);
        } catch (IOException | OAuthCommunicationException | OAuthExpectationFailedException | OAuthMessageSignerException e) {
            throw new HttpClientException("Error executing POST request " +
                "to:" + clientToString() + "/" + path + " streamable:" + streamable, e);
        }
    }

    @Override
    public HttpResponse postStreamableRequest(String path, StreamableRequest streamable, Map<String, String> headers) throws HttpClientException {
        try {
            HttpPost post = new HttpPost(toURI(path));
            setRequestHeaders(headers, post);
            post.setEntity(new StreamableEntity(streamable));
            post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM);
            return execute(post);
        } catch (IOException | OAuthCommunicationException | OAuthExpectationFailedException | OAuthMessageSignerException e) {
            throw new HttpClientException("Error executing POST request " +
                "to:" + clientToString() + "/" + path + " streamable:" + streamable, e);
        }
    }

    static class StreamableEntity implements HttpEntity {
        private final StreamableRequest streamable;

        public StreamableEntity(StreamableRequest streamable) {
            this.streamable = streamable;
        }

        @Override
        public boolean isRepeatable() {
            return true;
        }

        @Override
        public boolean isChunked() {
            return true;
        }

        @Override
        public long getContentLength() {
            return -1;
        }

        @Override
        public Header getContentType() {
            return new BasicHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM);
        }

        @Override
        public Header getContentEncoding() {
            return null;
        }

        @Override
        public InputStream getContent() throws IOException, UnsupportedOperationException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void writeTo(OutputStream outstream) throws IOException {
            streamable.writeRequest(outstream);
        }

        @Override
        public boolean isStreaming() {
            return true;
        }

        @Override
        public void consumeContent() throws IOException {
        }
    }

    @Override
    public String toString() {
        return "ApacheHttpClient441BackedHttpClient{"
            + "client=" + client
            + ", headersForEveryRequest=" + headersForEveryRequest
            + '}';
    }

    private HttpResponse execute(HttpRequestBase requestBase) throws IOException,
        OAuthMessageSignerException,
        OAuthExpectationFailedException,
        OAuthCommunicationException {
        applyHeadersCommonToAllRequests(requestBase);
        byte[] responseBody;
        StatusLine statusLine = null;
        if (LOG.isTraceEnabled()) {
            LOG.startTimer(TIMER_NAME);
        }

        activeCount.incrementAndGet();
        CloseableHttpResponse response = null;
        try {
            response = client.execute(requestBase);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream responseBodyAsStream = response.getEntity().getContent();
            if (responseBodyAsStream != null) {
                IOUtils.copy(responseBodyAsStream, outputStream);
            }

            responseBody = outputStream.toByteArray();
            statusLine = response.getStatusLine();
            return new HttpResponse(statusLine.getStatusCode(), statusLine.getReasonPhrase(), responseBody);
        } finally {
            if (response != null) {
                HttpClientUtils.closeQuietly(response);
            }
            requestBase.reset();
            activeCount.decrementAndGet();
            if (LOG.isTraceEnabled()) {
                long elapsedTime = LOG.stopTimer(TIMER_NAME);
                StringBuilder httpInfo = new StringBuilder();
                if (statusLine != null) {
                    httpInfo
                        .append("Outbound ")
                        .append(statusLine.getProtocolVersion())
                        .append(" Status ")
                        .append(statusLine.getStatusCode());
                } else {
                    httpInfo.append("Exception sending request");
                }
                httpInfo
                    .append(" in ")
                    .append(elapsedTime)
                    .append(" ms ")
                    .append(requestBase.getMethod())
                    .append(" ")
                    .append(client)
                    .append(requestBase.getURI());
                LOG.trace(httpInfo.toString());
            }
        }
    }

    private void setRequestHeaders(Map<String, String> headers, HttpRequestBase requestBase) {
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                requestBase.setHeader(header.getKey(), header.getValue());
            }
        }
    }

    private void applyHeadersCommonToAllRequests(HttpRequestBase requestBase) throws OAuthMessageSignerException,
        OAuthExpectationFailedException, OAuthCommunicationException {
        for (Map.Entry<String, String> headerEntry : headersForEveryRequest.entrySet()) {
            requestBase.setHeader(headerEntry.getKey(), headerEntry.getValue());
        }

        if (oauthSigner != null) {
            oauthSigner.sign(requestBase);
        }
    }

}
