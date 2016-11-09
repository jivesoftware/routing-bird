package com.jivesoftware.os.routing.bird.http.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContexts;

/**
 * @author jonathan.colt
 */
public class HttpRequestHelperUtils {

    private static final int DEFAULT_SOCKET_TIMEOUT_MILLIS = 10_000;

    public static HttpRequestHelper buildRequestHelper(boolean sslEnable,
        boolean allowSelfSigendCerts,
        OAuthSigner signer,
        String host,
        int port) throws Exception {
        return buildRequestHelper(sslEnable, allowSelfSigendCerts, signer, host, port, DEFAULT_SOCKET_TIMEOUT_MILLIS);
    }

    public static HttpRequestHelper buildRequestHelper(boolean sslEnable,
        boolean allowSelfSigendCerts,
        OAuthSigner signer,
        String host,
        int port,
        int socketTimeoutInMillis) throws Exception {

        List<HttpClientConfiguration> configs = new ArrayList<>();
        HttpClientConfig httpClientConfig = HttpClientConfig.newBuilder().setSocketTimeoutInMillis(socketTimeoutInMillis).build();
        configs.add(httpClientConfig);
        if (sslEnable) {

            HttpClientSSLConfig.Builder builder = HttpClientSSLConfig.newBuilder();
            builder.setUseSSL(true);
            if (allowSelfSigendCerts) {
                SSLContext sslcontext = SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
                // Allow TLSv1 protocol only, use NoopHostnameVerifier to trust self-singed cert
                builder.setUseSslWithCustomSSLSocketFactory(new SSLConnectionSocketFactory(sslcontext,
                    new String[] { "TLSv1" }, null, new NoopHostnameVerifier()));
            }
            configs.add(builder.build());
        }

        HttpClientFactory httpClientFactory = new HttpClientFactoryProvider().createHttpClientFactory(configs, false);
        HttpClient httpClient = httpClientFactory.createClient(signer, host, port);
        HttpRequestHelper requestHelper = new HttpRequestHelper(httpClient, new ObjectMapper());
        return requestHelper;
    }

}
