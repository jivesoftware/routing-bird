package com.jivesoftware.os.routing.bird.http.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;

/**
 *
 * @author jonathan.colt
 */
public class HttpRequestHelperUtils {

    public static HttpRequestHelper buildRequestHelper(String host, int port) {
        HttpClientConfig httpClientConfig = HttpClientConfig.newBuilder().build();
        HttpClientFactory httpClientFactory = new HttpClientFactoryProvider().createHttpClientFactory(Arrays.<HttpClientConfiguration>asList(httpClientConfig));
        HttpClient httpClient = httpClientFactory.createClient(host, port);
        HttpRequestHelper requestHelper = new HttpRequestHelper(httpClient, new ObjectMapper());
        return requestHelper;
    }
}
