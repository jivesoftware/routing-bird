package com.jivesoftware.os.routing.bird.server.oauth;

import com.jivesoftware.os.routing.bird.shared.OAuthPublicKey;

/**
 *
 * @author jonathan.colt
 */
public interface OAuthPublicKeyProvider {

    OAuthPublicKey lookupPublicKey(String id) throws AuthRequestException;

    long removalsTimestampMillis() throws Exception;
}
