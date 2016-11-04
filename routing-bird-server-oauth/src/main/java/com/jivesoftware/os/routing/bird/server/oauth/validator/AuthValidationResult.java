package com.jivesoftware.os.routing.bird.server.oauth.validator;

/**
 *
 */
public class AuthValidationResult {

    public final String consumerKey;
    public final boolean authorized;

    public AuthValidationResult(String consumerKey, boolean authorized) {
        this.consumerKey = consumerKey;
        this.authorized = authorized;
    }
}
