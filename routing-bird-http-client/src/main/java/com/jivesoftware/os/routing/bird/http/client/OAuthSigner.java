package com.jivesoftware.os.routing.bird.http.client;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.http.HttpRequest;
import org.apache.http.client.methods.HttpRequestBase;

/**
 *
 * @author jonathan.colt
 */
public interface OAuthSigner {

    HttpRequest sign(HttpRequestBase request) throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException;
}
