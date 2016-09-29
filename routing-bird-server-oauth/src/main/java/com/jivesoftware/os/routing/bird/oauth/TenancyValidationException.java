/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.routing.bird.oauth;

/**
 *
 */
public class TenancyValidationException extends Exception {

    public TenancyValidationException(String message) {
        super(message);
    }

    public TenancyValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}