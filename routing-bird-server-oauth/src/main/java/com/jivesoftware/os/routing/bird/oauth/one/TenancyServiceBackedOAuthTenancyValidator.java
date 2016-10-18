/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.routing.bird.oauth.one;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.oauth.TenancyValidationException;
import com.jivesoftware.os.routing.bird.oauth.TenancyValidator;
import java.util.List;
import org.glassfish.jersey.oauth1.signature.OAuth1Parameters;
import org.glassfish.jersey.oauth1.signature.OAuth1Request;
import org.glassfish.jersey.oauth1.signature.OAuth1Secrets;
import org.glassfish.jersey.oauth1.signature.OAuth1Signature;
import org.glassfish.jersey.oauth1.signature.OAuth1SignatureException;

/**
 * Note that to fully implement OAuth, we need to check for nonce-timestamp
 * uniqueness. We are deliberately deferring this to a future release.
 * <p>
 * <p>
 * When behind a loadbalancer you will likely need to add the following rules.
 * <p>
 * HTTP_HTTPS_X-Forward_set_header
 * <p>
 * This is needed in the LB for this to work! when HTTP_REQUEST { if
 * {([TCP::local_port] == 80) and !( [HTTP::header "X-Forwarded-Proto"] eq
 * "http") }{ HTTP::header insert X-Forwarded-Proto "http" HTTP::header insert
 * X-Forwarded-Port "80" } elseif {([TCP::local_port] == 443) and !(
 * [HTTP::header "X-Forwarded-Proto"] eq "https") } { HTTP::header insert
 * X-Forwarded-Proto "https" HTTP::header insert X-Forwarded-Port "443" } }
 */
public class TenancyServiceBackedOAuthTenancyValidator implements TenancyValidator<OAuth1Signature, OAuth1Request> {

    public final static String LB_PROTO_HEADER = "x-forwarded-proto";
    private final static MetricLogger log = MetricLoggerFactory.getLogger();
    private final long timestampAgeLimitMillis;
    private final List<TenancyServiceBackedSecretManager> secretManagers;
    private final Boolean doLoadBalancerRejiggering;

    public TenancyServiceBackedOAuthTenancyValidator(List<TenancyServiceBackedSecretManager> secretManagers,
        long timestampAgeLimitMillis,
        Boolean loadBalancerRejiggering) {
        this.timestampAgeLimitMillis = timestampAgeLimitMillis;
        this.secretManagers = secretManagers;
        this.doLoadBalancerRejiggering = (loadBalancerRejiggering == null) ? Boolean.FALSE : loadBalancerRejiggering;
    }

    @Override
    public boolean isValid(String tenantId, OAuth1Signature oAuth1Signature, OAuth1Request request) throws TenancyValidationException {
        if (doLoadBalancerRejiggering) {
            log.trace("request will be rejiggered");
            log.inc("oauth>rejiggeredRequest");
            List<String> originalProtocol = request.getHeaderValues(LB_PROTO_HEADER);
            if (originalProtocol != null && !originalProtocol.isEmpty()) {
                request = new OAuthHttpRequestRejiggering(request, originalProtocol.get(0));
            }
        }

        OAuth1Parameters params = new OAuth1Parameters();
        params.readRequest(request);

        String consumerKey = params.getConsumerKey();
        if ((consumerKey == null) || !consumerKey.equals(tenantId)) {
            log.warn("consumerKey:{} is null or not equal to tenantId:{}", consumerKey, tenantId);
            log.inc("oauth>error>consumerKeyNotEqual");
            log.inc("oauth>tenant>" + tenantId + ">error>consumerKeyNotEqual");
            throw new TenancyValidationException("Failed consumer key is not equal to tenantId:" + tenantId);
        }

        // Check that the timestamp has not expired. Note: oauth timestamp is in seconds ...
        String timestampStr = params.getTimestamp();
        long oauthTimeStamp = Long.parseLong(timestampStr) * 1000L;
        long now = System.currentTimeMillis();
        if (Math.abs(now - oauthTimeStamp) > timestampAgeLimitMillis) {
            log.warn("Timestamp out of range. timestamp:{}msec delta:{}msec", oauthTimeStamp, now - oauthTimeStamp);
            log.inc("oauth>error>outsideTimeRange");
            log.inc("oauth>tenant>" + tenantId + ">error>outsideTimeRange");
            throw new TenancyValidationException("The request timestamp is outside the allowable range. Please ensure you are running NTP.");
        }

        for (TenancyServiceBackedSecretManager secretManager : secretManagers) {
            String secret = secretManager.getSecret(tenantId);
            if (secret == null) {
                log.warn("secret for tenantId:{} is null", tenantId);
                log.inc("oauth>secrets>missing");
                log.inc("oauth>tenant>" + tenantId + ">secrets>missing");
                throw new TenancyValidationException("Failed to locate secret for tenantId:" + tenantId);
            }

            OAuth1Secrets secrets = new OAuth1Secrets();
            secrets.setConsumerSecret(secret);
            secrets.setTokenSecret(secret);

            try {
                boolean verify = oAuth1Signature.verify(request, params, secrets);
                if (verify) {
                    return true;
                } else {
                    log.warn("OAuth signature verification failed.");
                    log.inc("oauth>error>verificationFailed");
                    log.inc("oauth>tenant>" + tenantId + ">error>verificationFailed");
                }
            } catch (OAuth1SignatureException e) {
                log.warn("OAuth signature verification failed. {}", e.getClass().getSimpleName());
                log.inc("oauth>error>verificationError");
                log.inc("oauth>tenant>" + tenantId + ">error>verificationError");
                throw new TenancyValidationException("Oauth signature verification error.");
            }
        }
        return false;
    }

    public void expireSecretCacheIfNecessary() {
        for (TenancyServiceBackedSecretManager secretManager : secretManagers) {
            secretManager.verifyLastSecretRemovalTime();
        }
    }

    @Override
    public void clearSecretCache() {
        for (TenancyServiceBackedSecretManager secretManager : secretManagers) {
            secretManager.clearCache();
        }
    }
}
