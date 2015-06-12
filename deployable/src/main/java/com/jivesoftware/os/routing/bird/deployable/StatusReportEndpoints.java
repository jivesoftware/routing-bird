/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.routing.bird.deployable;

import com.google.inject.Singleton;
import com.jivesoftware.os.routing.bird.shared.ResponseHelper;
import com.jivesoftware.os.upena.reporter.service.StatusReportBroadcaster;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Singleton
@Path ("/announcement")
public class StatusReportEndpoints {

    private final StatusReportBroadcaster statusReportBroadcaster;

    public StatusReportEndpoints(@Context StatusReportBroadcaster statusReportBroadcaster) {
        this.statusReportBroadcaster = statusReportBroadcaster;
    }

    @GET
    @Path ("/json")
    public Response jsonStatusReport() {
        try {

            return ResponseHelper.INSTANCE.jsonResponse(statusReportBroadcaster.get());
        } catch (Exception x) {
            return ResponseHelper.INSTANCE.errorResponse("Failed to load properties.", x);
        }
    }
}
