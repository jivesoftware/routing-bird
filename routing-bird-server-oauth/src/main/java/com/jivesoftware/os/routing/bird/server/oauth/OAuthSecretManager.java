package com.jivesoftware.os.routing.bird.server.oauth;

/**
 *
 * @author jonathan.colt
 */
public interface OAuthSecretManager {

    void clearCache();

    String getSecret(String id) throws AuthValidationException;

    void verifyLastSecretRemovalTime() throws Exception;

}
