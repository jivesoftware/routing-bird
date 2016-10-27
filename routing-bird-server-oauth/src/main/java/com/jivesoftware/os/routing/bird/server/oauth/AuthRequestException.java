package com.jivesoftware.os.routing.bird.server.oauth;

public class AuthRequestException extends Exception {
    public AuthRequestException(String message) {
        super(message);
    }

    public AuthRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
