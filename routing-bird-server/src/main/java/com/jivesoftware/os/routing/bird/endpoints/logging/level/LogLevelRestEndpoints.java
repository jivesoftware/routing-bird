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
package com.jivesoftware.os.routing.bird.endpoints.logging.level;

import com.google.inject.Singleton;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

@Singleton
@Path("/logging")
public class LogLevelRestEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    @GET
    @Path("/listLogLevels")
    public Response listLogLevels() {
        StringBuilder sb = new StringBuilder();
        for (JsonLogLevel l : getLogLevels("null").getLogLevels()) {
            sb.append(l.getLoggerName()).append('=').append(l.getLoggerLevel()).append('\n');
        }
        return Response.ok(sb.toString(), MediaType.TEXT_PLAIN).build();
    }

    @GET
    @Consumes("application/json")
    @Path("/setLogLevel")
    public Response setLogLevel(
        @QueryParam("logger") @DefaultValue("") String loggerName,
        @QueryParam("level") @DefaultValue("null") String loggerLevel) {

        changeLogLevel(loggerName, loggerLevel);
        return Response.ok(loggerName + " set to " + loggerLevel, MediaType.TEXT_PLAIN).build();
    }

    private void changeLogLevel(String loggerName, String loggerLevel) {
        Level level = null;
        if (!loggerLevel.equals("null")) {
            level = Level.toLevel(loggerLevel);
        }

        LOG.info("Set metricLogger={} to level={}", loggerName, level);
        MetricLoggerFactory.setLogLevel(loggerName, false, level);

        org.apache.logging.log4j.spi.LoggerContext context = LogManager.getContext(false);
        if (context instanceof LoggerContext) {
            LoggerContext ctx = (LoggerContext) context;
            Configuration config = ctx.getConfiguration();
            LoggerConfig loggerConfig = config.getLoggerConfig(loggerName);
            if (!loggerConfig.getName().equals(loggerName)) {
                LOG.warn("Creating new logger config for '{}' because nearest config was '{}'", loggerName, loggerConfig.getName());
                loggerConfig = new LoggerConfig(loggerName, level, true);
                config.addLogger(loggerName, loggerConfig);
            }

            LOG.info("Set loggerConfig={} to level={}", loggerConfig.getName(), level);
            loggerConfig.setLevel(level);
            ctx.updateLoggers();
        } else {
            LOG.warn("Cannot get logger config because logger context is not an instance of org.apache.logging.log4j.core.LoggerContext");
        }

        Logger logger = LogManager.getLogger(loggerName);
        if (logger instanceof org.apache.logging.log4j.core.Logger) {
            ((org.apache.logging.log4j.core.Logger) logger).setLevel(level);
            LOG.info("Set logger={} to level={}", logger.getName(), level);
        } else {
            LOG.warn("Cannot get log level because root logger is not an instance of org.apache.logging.log4j.core.Logger");
        }
    }

    @POST
    @Consumes("application/json")
    @Path("/getLevels")
    @Produces("application/json")
    public JsonLogLevels getLogLevels(String tenantId) {

        List<JsonLogLevel> logLevels = new ArrayList<>();

        Logger rootLogger = LogManager.getRootLogger();
        if (rootLogger instanceof org.apache.logging.log4j.core.Logger) {
            LoggerContext lc = ((org.apache.logging.log4j.core.Logger) rootLogger).getContext();
            Collection<org.apache.logging.log4j.core.Logger> loggers = lc.getLoggers();
            for (Logger logger : loggers) {
                addToLogLevels(logger, logLevels);
            }
        } else {
            LOG.warn("Cannot get log level because root logger is not an instance of org.apache.logging.log4j.core.Logger");
        }
        addToLogLevels(rootLogger, logLevels);
        return new JsonLogLevels(tenantId, logLevels);
    }

    private void addToLogLevels(Logger logger,
        List<JsonLogLevel> logLevels) {
        String level = (logger.getLevel() == null) ? null : logger.getLevel().toString();
        logLevels.add(new JsonLogLevel(logger.getName(), level));
    }

    @POST
    @Consumes("application/json")
    @Path("/setLogLevels")
    public void setLogLevels(JsonLogLevels jsonLogLevels) {
        for (JsonLogLevel l : jsonLogLevels.getLogLevels()) {
            changeLogLevel(l.getLoggerName(), l.getLoggerLevel());
        }
    }

}
