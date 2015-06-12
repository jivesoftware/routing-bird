package com.jivesoftware.os.routing.bird.http.client;

/**
 *
 */
public class NonSuccessStatusCodeException extends RuntimeException {

    private final int statusCode;

    public NonSuccessStatusCodeException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}

