/*
 * Copyright 2013 Jive Software, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jivesoftware.os.routing.bird.endpoints.logging.metric;

import com.google.inject.Singleton;
import com.jivesoftware.os.mlogger.core.Counter;
import com.jivesoftware.os.mlogger.core.CountersAndTimers;
import com.jivesoftware.os.mlogger.core.LoggerSummary;
import com.jivesoftware.os.mlogger.core.TenantMetricStream;
import com.jivesoftware.os.routing.bird.endpoints.logging.metric.LoggerMetrics.MetricsStream;
import com.jivesoftware.os.routing.bird.shared.ResponseHelper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Singleton
@Path("/logging/metric")
public class LogMetricRestfulEndpoints {

    @GET
    @Path("/listTenantMetrics")
    public Response listTenantCounters(@QueryParam("tenant") @DefaultValue("") String tenant, @QueryParam("callback") @DefaultValue("") String callback) {

        try {

            final Metrics metrics = new Metrics();
            TenantMetricStream tenantMetricStream = (tenant1, cat) -> {
                if (cat != null) {
                    for (Entry<String, Counter> c : cat.getCounters()) {
                        metrics.metrics.add(new KeyAndMetric(c.getKey(), c.getValue().getValue()));
                    }
                }
                return true;
            };
            Collection<CountersAndTimers> all = CountersAndTimers.getAll();
            for (CountersAndTimers a : all) {
                if (tenant.isEmpty()) {
                    a.streamAllTenantMetrics(tenantMetricStream);
                } else {
                    a.streamTenantMetrics(tenant, tenantMetricStream);
                }
            }

            if (callback.length() > 0) {
                return ResponseHelper.INSTANCE.jsonpResponse(callback, metrics);
            } else {
                return ResponseHelper.INSTANCE.jsonResponse(metrics);
            }

        } catch (Exception ex) {
            return ResponseHelper.INSTANCE.errorResponse("Failed to list counters.", ex);
        }

    }

    @GET
    @Path("/listCounters")
    public Response listCounters(@QueryParam("logger") @DefaultValue("ALL") String loggerName, @QueryParam("callback") @DefaultValue("") String callback) {

        try {
            if (loggerName.equals("ALL")) {
                loggerName = "";
            }
            final Metrics metrics = new Metrics();
            MetricsHelper.INSTANCE.getCounters(loggerName).getAll(new MetricsStream() {

                @Override
                public void callback(Entry<String, Long> v) throws Exception {
                    if (v != null) {
                        metrics.metrics.add(new KeyAndMetric(v.getKey(), v.getValue()));
                    }
                }
            });
            if (callback.length() > 0) {
                return ResponseHelper.INSTANCE.jsonpResponse(callback, metrics);
            } else {
                return ResponseHelper.INSTANCE.jsonResponse(metrics);
            }

        } catch (Exception ex) {
            return ResponseHelper.INSTANCE.errorResponse("Failed to list counters.", ex);
        }

    }

    @GET
    @Path("/listTimers")
    public Response listTimers(@QueryParam("logger") @DefaultValue("ALL") String loggerName, @QueryParam("callback") @DefaultValue("") String callback) {
        try {
            if (loggerName.equals("ALL")) {
                loggerName = "";
            }

            final Metrics metrics = new Metrics();
            MetricsHelper.INSTANCE.getTimers(loggerName).getAll(new MetricsStream() {

                @Override
                public void callback(Entry<String, Long> v) throws Exception {
                    if (v != null) {
                        metrics.metrics.add(new KeyAndMetric(v.getKey(), v.getValue()));
                    }
                }
            });
            if (callback.length() > 0) {
                return ResponseHelper.INSTANCE.jsonpResponse(callback, metrics);
            } else {
                return ResponseHelper.INSTANCE.jsonResponse(metrics);
            }
        } catch (Exception ex) {
            return ResponseHelper.INSTANCE.errorResponse("Failed to list timers.", ex);
        }
    }

    @GET
    @Path("/resetCounter")
    public String resetCounter() {
        CountersAndTimers.resetAll();
        LoggerSummary.INSTANCE.reset();
        return "counter were reset.";
    }

    static class Metrics {

        public List<KeyAndMetric> metrics = new ArrayList<>();

        public Metrics() {
        }
    }

    static class KeyAndMetric {

        public String key;
        public double metric;

        public KeyAndMetric() {
        }

        public KeyAndMetric(String key, double metric) {
            this.key = key;
            this.metric = metric;
        }
    }

}
