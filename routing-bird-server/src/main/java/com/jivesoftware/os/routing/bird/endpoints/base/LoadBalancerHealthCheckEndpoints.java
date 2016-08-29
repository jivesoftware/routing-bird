package com.jivesoftware.os.routing.bird.endpoints.base;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 *
 * @author jonathan.colt
 */
@Singleton
@Path("/health/check")
public class LoadBalancerHealthCheckEndpoints {
    @GET
    public Response check() {
        return Response.ok().build();
    }
}
