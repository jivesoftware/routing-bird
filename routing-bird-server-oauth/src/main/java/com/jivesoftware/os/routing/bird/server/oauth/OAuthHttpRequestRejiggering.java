/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.routing.bird.server.oauth;

import java.net.URL;
import java.util.List;
import java.util.Set;
import org.glassfish.jersey.oauth1.signature.OAuth1Request;

/**
 *
 */
public class OAuthHttpRequestRejiggering implements OAuth1Request {

    private final OAuth1Request oAuth1Request;
    private final String originalProtocol;
    private final int originalPort;

    public OAuthHttpRequestRejiggering(OAuth1Request oAuth1Request, String originalProtocol, int originalPort) {
        this.oAuth1Request = oAuth1Request;
        this.originalProtocol = originalProtocol;
        this.originalPort = originalPort;
    }

    @Override
    public String getRequestMethod() {
        return oAuth1Request.getRequestMethod();
    }

    @Override
    public URL getRequestURL() {
        try {
            URL url = oAuth1Request.getRequestURL();
            return new URL(originalProtocol,
                url.getHost(),
                originalPort == -1 ? url.getPort() : originalPort,
                url.getPath());
        } catch (Exception x) {
            throw new RuntimeException("Failed to rejigger url", x);
        }
    }

    @Override
    public Set<String> getParameterNames() {
        return oAuth1Request.getParameterNames();
    }

    @Override
    public List<String> getParameterValues(String name) {
        return oAuth1Request.getParameterValues(name);
    }

    @Override
    public List<String> getHeaderValues(String name) {
        return oAuth1Request.getHeaderValues(name);
    }

    @Override
    public void addHeaderValue(String name, String value) throws IllegalStateException {
        oAuth1Request.addHeaderValue(name, value);
    }

}