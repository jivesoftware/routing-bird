/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.routing.bird.server.oauth;

/**
 *
 */
public class AuthValidationException extends Exception {

    public AuthValidationException(String message) {
        super(message);
    }

    public AuthValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
