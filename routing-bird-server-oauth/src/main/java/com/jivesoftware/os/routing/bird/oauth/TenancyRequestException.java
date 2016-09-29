package com.jivesoftware.os.routing.bird.oauth;

/**
 * This exception is thrown when tenancy validator could not successfully communicate to tenancy service
 */
public class TenancyRequestException extends Exception {
    public TenancyRequestException(String message) {
        super(message);
    }

    public TenancyRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
