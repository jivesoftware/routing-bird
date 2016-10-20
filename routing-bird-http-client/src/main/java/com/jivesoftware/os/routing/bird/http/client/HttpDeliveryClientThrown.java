package com.jivesoftware.os.routing.bird.http.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.shared.InstanceThrown;
import com.jivesoftware.os.routing.bird.shared.InstanceThrowns;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author jonathan.colt
 */
public class HttpDeliveryClientThrown implements Runnable {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final String instanceId;
    private final HttpRequestHelper httpRequestHelper;
    private final String path;
    private final long interval;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    public HttpDeliveryClientThrown(String instanceId, HttpRequestHelper httpRequestHelper, String path, long interval) {
        this.instanceId = instanceId;
        this.httpRequestHelper = httpRequestHelper;
        this.path = path;
        this.interval = interval;
    }

    public void start() {
        scheduledExecutorService.scheduleAtFixedRate(this, interval, interval, TimeUnit.MILLISECONDS);
    }

    public void stop() throws InterruptedException {
        if (httpRequestHelper != null) {
            scheduledExecutorService.shutdownNow();
            scheduledExecutorService.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    @Override
    public void run() {
        try {
            List<InstanceThrown> throwns = new ArrayList<>();
//            throwns

            httpRequestHelper.executeRequest(new InstanceThrowns(instanceId, throwns), path, String.class, null);
        } catch (Exception x) {
            LOG.warn("Failed to deliver client health.", x);
        }
    }


    static HttpRequestHelper buildRequestHelper(OAuthSigner signer, String host, int port) {
        HttpClientConfig httpClientConfig = HttpClientConfig.newBuilder().build();
        HttpClientFactory httpClientFactory = new HttpClientFactoryProvider().createHttpClientFactory(Arrays.<HttpClientConfiguration>asList(httpClientConfig),
            false);
        HttpClient httpClient = httpClientFactory.createClient(signer, host, port);
        HttpRequestHelper requestHelper = new HttpRequestHelper(httpClient, new ObjectMapper());
        return requestHelper;
    }

}
