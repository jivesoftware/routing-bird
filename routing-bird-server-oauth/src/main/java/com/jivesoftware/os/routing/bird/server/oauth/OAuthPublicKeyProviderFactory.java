package com.jivesoftware.os.routing.bird.server.oauth;

import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelper;

/**
 *
 * @author jonathan.colt
 */
public interface OAuthPublicKeyProviderFactory {

    OAuthPublicKeyProvider create(HttpRequestHelper httpRequestHelper) throws Exception;
}
