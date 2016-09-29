/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.routing.bird.oauth.one;

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

    public OAuthHttpRequestRejiggering(OAuth1Request oAuth1Request, String originalProtocol) {
        this.oAuth1Request = oAuth1Request;
        this.originalProtocol = originalProtocol;
    }

    @Override
    public String getRequestMethod() {
        return oAuth1Request.getRequestMethod();
    }

    @Override
    public URL getRequestURL() {
        try {
            URL url = oAuth1Request.getRequestURL();
            return new URL(originalProtocol, url.getHost(), url.getPort(), url.getPath());
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

// Note: url.getProtocol() == uri.getScheme() == (in this case) 'http' or 'https' ...
//
//    @Override
//    public URI getRequestUri() {
//        // Note: getRequestUri().toURL() + url.getProtocol() is called by the OAuth signature builder.
//        // overriding just this method should be adequate.
//
//        UriBuilder builder = oAuth1Request.getRequestUriBuilder();
//        builder.scheme(originalProtocol);
//        return builder.build();
//    }
//
//    @Override
//    public UriBuilder getRequestUriBuilder() {
//        // Just in case, I'm changing the protocol here as well ....
//        return oAuth1Request.getRequestUriBuilder().scheme(originalProtocol);
//    }
//
//
//    @Override
//    public URI getBaseUri() {
//        return oAuth1Request.getBaseUri();
//    }
//
//    @Override
//    public UriBuilder getBaseUriBuilder() {
//        return oAuth1Request.getBaseUriBuilder();
//    }
//
//    @Override
//    public URI getAbsolutePath() {
//        return oAuth1Request.getAbsolutePath();
//    }
//
//    @Override
//    public UriBuilder getAbsolutePathBuilder() {
//        return oAuth1Request.getAbsolutePathBuilder();
//    }
//
//    @Override
//    public String getPath() {
//        return oAuth1Request.getPath();
//    }
//
//    @Override
//    public String getPath(boolean bln) {
//        return oAuth1Request.getPath(bln);
//    }
//
//    @Override
//    public List<PathSegment> getPathSegments() {
//        return oAuth1Request.getPathSegments();
//    }
//
//    @Override
//    public List<PathSegment> getPathSegments(boolean bln) {
//        return oAuth1Request.getPathSegments(bln);
//    }
//
//    @Override
//    public MultivaluedMap<String, String> getQueryParameters() {
//        return oAuth1Request.getQueryParameters();
//    }
//
//    @Override
//    public MultivaluedMap<String, String> getQueryParameters(boolean bln) {
//        return oAuth1Request.getQueryParameters(bln);
//    }
//
//    @Override
//    public String getHeaderValue(String string) {
//        return oAuth1Request.getHeaderValue(string);
//    }
//
//    @Override
//    public MediaType getAcceptableMediaType(List<MediaType> list) {
//        return oAuth1Request.getAcceptableMediaType(list);
//    }
//
//    @Override
//    public List<MediaType> getAcceptableMediaTypes(List<QualitySourceMediaType> list) {
//        return oAuth1Request.getAcceptableMediaTypes(list);
//    }
//
//    @Override
//    public MultivaluedMap<String, String> getCookieNameValueMap() {
//        return oAuth1Request.getCookieNameValueMap();
//    }
//
//    @Override
//    public <T> T getEntity(Class<T> type) throws WebApplicationException {
//        return oAuth1Request.getEntity(type);
//    }
//
//    @Override
//    public <T> T getEntity(Class<T> type, Type type1, Annotation[] antns) throws WebApplicationException {
//        return oAuth1Request.getEntity(type, type1, antns);
//    }
//
//    @Override
//    public Form getFormParameters() {
//        return oAuth1Request.getFormParameters();
//    }
//
//    @Override
//    public List<String> getRequestHeader(String name) {
//        return oAuth1Request.getRequestHeader(name);
//    }
//
//    @Override
//    public MultivaluedMap<String, String> getRequestHeaders() {
//        return oAuth1Request.getRequestHeaders();
//    }
//
//    @Override
//    public List<MediaType> getAcceptableMediaTypes() {
//        return oAuth1Request.getAcceptableMediaTypes();
//    }
//
//    @Override
//    public List<Locale> getAcceptableLanguages() {
//        return oAuth1Request.getAcceptableLanguages();
//    }
//
//    @Override
//    public MediaType getMediaType() {
//        return oAuth1Request.getMediaType();
//    }
//
//    @Override
//    public Locale getLanguage() {
//        return oAuth1Request.getLanguage();
//    }
//
//    @Override
//    public Map<String, Cookie> getCookies() {
//        return oAuth1Request.getCookies();
//    }
//
//    @Override
//    public String getMethod() {
//        return oAuth1Request.getMethod();
//    }
//
//    @Override
//    public Variant selectVariant(List<Variant> variants) throws IllegalArgumentException {
//        return oAuth1Request.selectVariant(variants);
//    }
//
//    @Override
//    public ResponseBuilder evaluatePreconditions(EntityTag eTag) {
//        return oAuth1Request.evaluatePreconditions(eTag);
//    }
//
//    @Override
//    public ResponseBuilder evaluatePreconditions(Date lastModified) {
//        return oAuth1Request.evaluatePreconditions(lastModified);
//    }
//
//    @Override
//    public ResponseBuilder evaluatePreconditions(Date lastModified, EntityTag eTag) {
//        return oAuth1Request.evaluatePreconditions(lastModified, eTag);
//    }
//
//    @Override
//    public ResponseBuilder evaluatePreconditions() {
//        return oAuth1Request.evaluatePreconditions();
//    }
//
//    @Override
//    public Principal getUserPrincipal() {
//        return oAuth1Request.getUserPrincipal();
//    }
//
//    @Override
//    public boolean isUserInRole(String role) {
//        return oAuth1Request.isUserInRole(role);
//    }
//
//    @Override
//    public boolean isSecure() {
//        return oAuth1Request.isSecure();
//    }
//
//    @Override
//    public String getAuthenticationScheme() {
//        return oAuth1Request.getAuthenticationScheme();
//    }
//
//    @Override
//    public boolean isTracingEnabled() {
//        return oAuth1Request.isTracingEnabled();
//    }
//
//    @Override
//    public void trace(String string) {
//        oAuth1Request.trace(string);
//    }
//
//}
/**
 * This is a snippent from Uttam on how they fixed load balancer header termination fun.
    HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request) {
            @Override
            public StringBuffer getRequestURL() {
                StringBuffer url = super.getRequestURL();


                //Code from Murali...
                // If the load balancer drops ssl while forwarding the request, then oauth validation fails
                //   REASON: The request is signed with https, but validated with http.
                //   Following check is an attempt to fix the url with valid scheme

                //  The EC2 load balancer injects header x-forwarded-proto to the request, exposing the original scheme,
                //   using that value to update the url
                String lbProtoHeader = super.getHeader(LB_PROTO_HEADER);
                if (lbProtoHeader != null && !super.getScheme().equalsIgnoreCase(lbProtoHeader)) {
                    url = new StringBuffer(url.toString().replaceFirst("^https?:", lbProtoHeader + ':'));
                }

                String path = super.getServletPath();
                String protocol = url.substring(0, url.indexOf(path));
                String uri = url.substring(url.indexOf(path));
                uri = uri.replace(":", "%3A");

                url = new StringBuffer(protocol).append(uri);
                return url;
            }

            @Override
            public String getScheme() {
                String lbProtoHeader = super.getHeader(LB_PROTO_HEADER);
                return (lbProtoHeader == null) ? super.getScheme() : lbProtoHeader;
            }
        };

        return wrapper;
        }
 */
