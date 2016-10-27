package com.jivesoftware.os.routing.bird.server.oauth.request;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.glassfish.jersey.oauth1.signature.OAuth1Request;

/**
 *
 * @author jonathan.colt
 */
public class HttpServletRequestOAuth1Request implements OAuth1Request {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final HttpServletRequest request;

    public HttpServletRequestOAuth1Request(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public String getRequestMethod() {
        return request.getMethod();
    }

    @Override
    public URL getRequestURL() {
        try {
            return new URL(request.getRequestURL().toString());
        } catch (MalformedURLException ex) {
            LOG.error(request.getRequestURL() + " was malformed.", ex);
            return null;
        }
    }

    @Override
    public Set<String> getParameterNames() {
        return request.getParameterMap().keySet();
    }

    @Override
    public List<String> getParameterValues(String name) {
        return Arrays.asList(request.getParameterValues(name));
    }

    @Override
    public List<String> getHeaderValues(String name) {
        return Collections.list(request.getHeaders(name));
    }

    @Override
    public void addHeaderValue(String name, String value) throws IllegalStateException {
        throw new IllegalStateException("Modifying OAuthServerRequest unsupported");
    }

}
