package com.jivesoftware.os.routing.bird.server.oauth.request;

import com.google.common.collect.Sets;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import javax.ws.rs.container.ContainerRequestContext;
import org.glassfish.jersey.oauth1.signature.OAuth1Request;

/**
 *
 * @author jonathan.colt
 */
public class ContainerRequestContextOAuth1Request implements OAuth1Request {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final ContainerRequestContext request;

    public ContainerRequestContextOAuth1Request(ContainerRequestContext request) {
        this.request = request;
    }

    @Override
    public String getRequestMethod() {
        return request.getMethod();
    }

    @Override
    public URL getRequestURL() {
        try {
            return request.getUriInfo().getRequestUri().toURL();
        } catch (MalformedURLException ex) {
            LOG.error(request.getUriInfo().getRequestUri() + " was malformed.", ex);
            return null;
        }
    }

    @Override
    public Set<String> getParameterNames() {
        Set<String> parameterNames = Sets.newHashSet();
        parameterNames.addAll(request.getUriInfo().getQueryParameters().keySet());
        //TODO form params
        return parameterNames;
    }

    @Override
    public List<String> getParameterValues(String name) {
        List<String> values = request.getUriInfo().getPathParameters().get(name);
        if (values == null || values.isEmpty()) {
            values = request.getUriInfo().getQueryParameters().get(name);
        }
        return values;
    }

    @Override
    public List<String> getHeaderValues(String name) {
        return request.getHeaders().get(name);
    }

    @Override
    public void addHeaderValue(String name, String value) throws IllegalStateException {
        throw new IllegalStateException("Modifying request is unsupported");
    }

}
