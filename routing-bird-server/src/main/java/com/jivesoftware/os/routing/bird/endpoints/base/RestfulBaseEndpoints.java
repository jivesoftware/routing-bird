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
package com.jivesoftware.os.routing.bird.endpoints.base;

import com.jivesoftware.os.mlogger.core.LoggerSummary;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.endpoints.logging.metric.MetricsHelper;
import com.jivesoftware.os.routing.bird.health.HealthCheckResponse;
import com.jivesoftware.os.routing.bird.health.HealthCheckService;
import com.jivesoftware.os.routing.bird.shared.ResponseHelper;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.rendersnake.HtmlAttributes;
import org.rendersnake.HtmlAttributesFactory;
import org.rendersnake.HtmlCanvas;

@Singleton
@Path("/")
public class RestfulBaseEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final HealthCheckService healthCheckService;
    private final File logFile;
    private final HasUI hasUI;
    private final FullyOnlineVersion fullyOnlineVersion;

    public RestfulBaseEndpoints(@Context HealthCheckService healthCheckService,
        @Context File logFile,
        @Context HasUI hasUI,
        @Context FullyOnlineVersion fullyOnlineVersion) {

        this.healthCheckService = healthCheckService;
        this.logFile = logFile;
        this.hasUI = hasUI;
        this.fullyOnlineVersion = fullyOnlineVersion;
    }

    @GET
    @Path("/hasUI")
    public Response hasUI() {
        return ResponseHelper.INSTANCE.jsonResponse(hasUI);
    }

    @GET
    @Path("/metrics/ui")
    public Response metricsUI() {
        try {
            final HtmlCanvas canvas = new HtmlCanvas();

            canvas.h1().content("Counters: ");
            HtmlAttributes tableAttributes = HtmlAttributesFactory
                .class_("table")
                .class_("table-striped")
                .class_("table-bordered")
                .class_("table-hover")
                .class_("table-condensed");

            canvas.table(tableAttributes);
            canvas.tr();
            canvas.td().content("Count");
            canvas.td().content("Name");
            canvas._tr();

            MetricsHelper.INSTANCE.getCounters("").getAll((v) -> {
                if (v != null) {
                    canvas.tr();
                    canvas.td().content(String.valueOf(v.getValue()));
                    canvas.td().content(v.getKey());
                    canvas._tr();
                }
            });

            canvas._table();

            canvas.hr();
            canvas.h1().content("Timers: ");
            tableAttributes = HtmlAttributesFactory
                .class_("table")
                .class_("table-striped")
                .class_("table-bordered")
                .class_("table-hover")
                .class_("table-condensed");

            canvas.table(tableAttributes);
            canvas.tr();
            canvas.td().content("Timer");
            canvas.td().content("Name");
            canvas._tr();

            MetricsHelper.INSTANCE.getTimers("").getAll((v) -> {
                if (v != null) {
                    canvas.tr();
                    canvas.td().content(String.valueOf(v.getValue()));
                    canvas.td().content(v.getKey());
                    canvas._tr();
                }
            });

            canvas._table();

            return Response.ok(canvas.toHtml(), MediaType.TEXT_HTML).build();
        } catch (Exception x) {
            LOG.warn("Failed build UI html.", x);
            return ResponseHelper.INSTANCE.errorResponse("Failed build UI html.", x);
        }
    }

    @GET
    @Path("/health/ui")
    public Response healthUI() {
        try {
            NumberFormat numberFormat = NumberFormat.getNumberInstance();
            HtmlCanvas canvas = new HtmlCanvas();
            List<HealthCheckResponse> checkHealth = healthCheckService.checkHealth();
            //table table-striped table-bordered table-hover table-condensed

            HtmlAttributes tableAttributes = HtmlAttributesFactory
                .class_("table")
                .class_("table-striped")
                .class_("table-bordered")
                .class_("table-hover")
                .class_("table-condensed");

            canvas.table(tableAttributes);
            canvas.th();
            canvas.tr();
            canvas.td().content(String.valueOf("Health"));
            canvas.td().content(String.valueOf("Name"));
            canvas.td().content(String.valueOf("Status"));
            canvas.td().content(String.valueOf("Description"));
            canvas.td().content(String.valueOf("Resolution"));
            canvas.td().content(String.valueOf("Age"));
            canvas._tr();
            canvas._th();

            canvas.tbody();
            long now = System.currentTimeMillis();
            for (HealthCheckResponse response : checkHealth) {
                if (-Double.MAX_VALUE != response.getHealth()) {
                    canvas.tr();
                    canvas.td(HtmlAttributesFactory.style("background-color:#" + getHEXTrafficlightColor(response.getHealth()) + ";"))
                        .content(String.valueOf(numberFormat.format(response.getHealth())));
                    canvas.td().content(String.valueOf(response.getName()));
                    canvas.td().content(String.valueOf(response.getStatus()));
                    canvas.td().content(String.valueOf(response.getDescription()));
                    canvas.td().content(String.valueOf(response.getResolution()));
                    canvas.td().content(String.valueOf(shortHumanReadableUptime(now - response.getTimestamp())));
                    canvas._tr();
                }
            }
            canvas._tbody();
            canvas._table();
            return Response.ok(canvas.toHtml(), MediaType.TEXT_HTML).build();
        } catch (Exception x) {
            LOG.warn("Failed build UI html.", x);
            return ResponseHelper.INSTANCE.errorResponse("Failed build UI html.", x);
        }
    }

    public static String shortHumanReadableUptime(long millis) {
        if (millis < 0) {
            return String.valueOf(millis);
        }

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder(64);
        if (days > 0) {
            sb.append(days + "d ");
        }
        if (hours > 0) {
            sb.append(hours + "h ");
        }
        if (minutes > 0) {
            sb.append(minutes + "m ");
        }
        if (seconds > 0) {
            sb.append(seconds + "s");
        }
        return sb.toString();
    }

    String getHEXTrafficlightColor(double value) {
        String s = Integer.toHexString(Color.HSBtoRGB((float) value / 3f, 1f, 1f) & 0xffffff);
        return "000000".substring(s.length()) + s;
    }

    @GET
    @Path("/forceGC")
    public Response forceGC() {
        Runtime.getRuntime().gc();
        return Response.ok("Forced GC", MediaType.TEXT_PLAIN).build();
    }

    @GET
    @Path("/threadDump")
    @Produces(MediaType.TEXT_PLAIN)
    public Response threadDump() {
        List<ThreadDump> threadDumps = new ArrayList<>();
        for (Map.Entry<Thread, StackTraceElement[]> trace : Thread.getAllStackTraces().entrySet()) {
            threadDumps.add(new ThreadDump(trace.getKey(), trace.getValue()));
        }
        Collections.sort(threadDumps, (o1, o2) -> Integer.compare(o1.thread.getState().ordinal(), o2.thread.getState().ordinal()));

        StringBuilder builder = new StringBuilder();
        for (ThreadDump threadDump : threadDumps) {
            builder.append(String.format("\"%s\" %s prio=%d tid=%d nid=1 %s\njava.lang.Thread.State: %s\n",
                threadDump.thread.getName(),
                (threadDump.thread.isDaemon() ? "daemon" : ""),
                threadDump.thread.getPriority(),
                threadDump.thread.getId(),
                Thread.State.WAITING.equals(threadDump.thread.getState()) ? "in Object.wait()" : threadDump.thread.getState().name().toLowerCase(),
                (threadDump.thread.getState().equals(Thread.State.WAITING) ? "WAITING (on object monitor)" : threadDump.thread.getState())));
            for (StackTraceElement stackTraceElement : threadDump.trace) {
                builder.append("\n        at ");
                builder.append(stackTraceElement);
            }
            builder.append("\n\n");
        }

        return Response.ok(builder.toString()).build();
    }

    static class ThreadDump {
        Thread thread;
        StackTraceElement[] trace;

        public ThreadDump(Thread thread, StackTraceElement[] trace) {
            this.thread = thread;
            this.trace = trace;
        }
    }

    @GET
    @Path("/resetErrors")
    public Response resetErrors() {
        LoggerSummary.INSTANCE.errors.reset();
        LoggerSummary.INSTANCE.lastNErrors.clear("");
        LoggerSummary.INSTANCE_EXTERNAL_INTERACTIONS.errors.reset();
        LoggerSummary.INSTANCE_EXTERNAL_INTERACTIONS.lastNErrors.clear("");
        return Response.ok("Reset Errors", MediaType.TEXT_PLAIN).build();
    }

    /**
     * Easy way to see the last ~10 errors, warns and infos
     *
     * @param nLines
     * @param callback
     * @return
     */
    @GET
    @Path("/tail")
    public Response tail(@QueryParam("format") @DefaultValue("text") String format,
        @QueryParam("lastNLines") @DefaultValue("1000") int nLines,
        @QueryParam("callback") @DefaultValue("") String callback) {
        if (callback.length() > 0) {
            return ResponseHelper.INSTANCE.jsonpResponse(callback, tailLogFile(logFile, 80 * nLines));
        } else {
            if (format.equals("text")) {
                return Response.ok(tailLogFile(logFile, 80 * nLines), MediaType.TEXT_PLAIN).build();
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append(tailLogFile(logFile, 80 * nLines));
                return ResponseHelper.INSTANCE.jsonResponse(sb.toString());
            }
        }
    }

    private String tailLogFile(File file, int lastNBytes) {
        try (RandomAccessFile fileHandler = new RandomAccessFile(file, "r")) {
            long fileLength = file.length() - 1;
            long start = fileLength - lastNBytes;
            if (start < 0) {
                start = 0;
            }
            byte[] bytes = new byte[(int) (fileLength - start)];
            fileHandler.seek(start);
            fileHandler.readFully(bytes);
            return new String(bytes, "ASCII");
        } catch (FileNotFoundException e) {
            LOG.warn("Tailing failed to locate file. " + file);
            return "Tailing failed to locate file. " + file;
        } catch (IOException e) {
            LOG.warn("Tailing file encountered the following error. " + file, e);
            return "Tailing file encountered the following error. " + file;
        }
    }

    @GET
    @Path("/recentErrors")
    public Response recentErrors(@QueryParam("callback") @DefaultValue("") String callback) {
        if (callback.length() > 0) {
            return ResponseHelper.INSTANCE.jsonpResponse(callback, LoggerSummary.INSTANCE.lastNErrors.get());
        } else {
            return ResponseHelper.INSTANCE.jsonResponse(LoggerSummary.INSTANCE.lastNErrors.get());
        }
    }

    class Health {
        public String version = "unknown";
        public boolean fullyOnline = false;
        public double health = 1.0d;
        public List<HealthCheckResponse> healthChecks = new ArrayList<>();
    }

    /**
     * Health of service
     *
     * @param callback
     * @return
     */
    @GET
    @Path("/health")
    public Response health(@QueryParam("callback") @DefaultValue("") String callback) {
        try {
            Health health = new Health();
            String version = fullyOnlineVersion.getFullyOnlineVersion();
            if (version == null) {
                health.fullyOnline = false;
                health.version = "still in startup";
            } else {
                health.fullyOnline = true;
                health.version = version;
            }

            health.healthChecks = healthCheckService.checkHealth();
            for (HealthCheckResponse response : health.healthChecks) {
                if (-Double.MAX_VALUE != response.getHealth()) {
                    health.health = Math.min(health.health, response.getHealth());
                }
            }

            Response.ResponseBuilder builder;
            if (health.health > 0.0d) {
                builder = Response.ok();
            } else {
                builder = Response.status(Response.Status.SERVICE_UNAVAILABLE);
            }
            if (callback.length() > 0) {
                return builder.entity(ResponseHelper.INSTANCE.jsonpResponse(callback, health).getEntity()).type(new MediaType("application", "javascript")).
                    build();
            } else {
                return builder.entity(health).type(MediaType.APPLICATION_JSON).build();
            }
        } catch (Exception x) {
            LOG.warn("Failed to get health.", x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to get health.", x);
        }
    }

    @GET
    @Path("/resetHealth")
    public Response resetHealth() {
        try {
            healthCheckService.resetHealthChecks();
            return Response.ok("Reset Health", MediaType.TEXT_PLAIN).build();
        } catch (Exception x) {
            LOG.warn("Failed to reset health.", x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to reset health.", x);
        }
    }

    @GET
    @Path("/resetThrown")
    public Response resetThrown() {
        try {
            LoggerSummary.INSTANCE.reset();
            return Response.ok("Reset LoggerSummary", MediaType.TEXT_PLAIN).build();
        } catch (Exception x) {
            LOG.warn("Failed to reset thrown.", x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to reset thrown.", x);
        }
    }

    @GET
    @Path("/logLevels/{instanceKey}")
    public Response logLevels(@PathParam("instanceKey") @DefaultValue("") String instanceKey) {
        try {
            HtmlCanvas canvas = new HtmlCanvas();

            canvas.form(HtmlAttributesFactory
                .action("/ui/deployable/setLogLevel/" + instanceKey)
                .method("post")
                .id("setLogLevel-form"));
            canvas.fieldset();

            org.apache.logging.log4j.Logger rl = LogManager.getRootLogger();
            if (rl instanceof Logger) {
                LoggerContext rlContext = ((Logger) rl).getContext();
                Collection<Logger> loggers = rlContext.getLoggers();

                canvas.select(HtmlAttributesFactory.name("logger"));
                for (Logger logger : loggers) {
                    try {
                        Class.forName(logger.getName());
                        String level = (logger.getLevel() == null) ? null : logger.getLevel().toString();
                        canvas.option(HtmlAttributesFactory.value(logger.getName())).content(level + "=" + logger.getName());
                    } catch (ClassNotFoundException e) {
                        LOG.warn("Failed to find class:{}", e.getLocalizedMessage());
                    }
                }
                canvas._select();
            } else {
                canvas.h1().content("Loggers unavailable, RootLogger is: " + rl.getClass().getName() + " expected: " + Logger.class.getName());
            }

            canvas.select(HtmlAttributesFactory.name("level"));
            canvas.option(HtmlAttributesFactory.value("")).content("Inherit");
            canvas.option(HtmlAttributesFactory.value("TRACE")).content("TRACE");
            canvas.option(HtmlAttributesFactory.value("DEBUG")).content("DEBUG");
            canvas.option(HtmlAttributesFactory.value("INFO")).content("INFO");
            canvas.option(HtmlAttributesFactory.value("WARN")).content("WARN");
            canvas.option(HtmlAttributesFactory.value("ERROR")).content("ERROR");
            canvas.option(HtmlAttributesFactory.value("OFF")).content("OFF");
            canvas._select();

            canvas.input(HtmlAttributesFactory.type("submit").value("Change"))
                ._fieldset()
                ._form();

            return Response.ok(canvas.toHtml(), MediaType.TEXT_HTML).build();
        } catch (Exception x) {
            LOG.warn("Failed to set logging level.", x);
            return ResponseHelper.INSTANCE.errorResponse("Failed to set logging level.", x);
        }
    }

}
