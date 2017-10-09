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
package com.jivesoftware.os.routing.bird.server;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Lists;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.server.binding.Injectable;
import com.jivesoftware.os.routing.bird.server.binding.InjectableBinder;
import com.jivesoftware.os.routing.bird.server.filter.NewRelicRequestFilter;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.DispatcherType;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.HttpMethodOverrideFilter;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 *
 */
public class JerseyEndpoints implements HasServletContextHandler {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final Set<Class<?>> allClasses = new HashSet<>();
    private final Set<Class<?>> allInjectedClasses = new HashSet<>();
    private final Set<Object> allBinders = new HashSet<>();
    private final List<Injectable<?>> allInjectables = Lists.newArrayList();
    private final List<ContainerRequestFilter> containerRequestFilters = Lists.newArrayList();
    private final List<ContainerResponseFilter> containerResponseFilters = Lists.newArrayList();
    private boolean supportCORS = false;
    private boolean enableSwagger = false;

    private final ObjectMapper mapper;

    public JerseyEndpoints() {
        this.mapper = new ObjectMapper()
            .configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
    }

    public JerseyEndpoints addEndpoint(Class<?> jerseyEndpoint) {
        allClasses.add(jerseyEndpoint);
        return this;
    }

    public JerseyEndpoints addInjectable(Object injectableInstance) {
        return addInjectable(Injectable.of(injectableInstance));
    }

    public JerseyEndpoints addInjectable(Class<?> injectableClass, Object injectableInstance) {
        return addInjectable(Injectable.ofUnsafe(injectableClass, injectableInstance));
    }

    public JerseyEndpoints addInjectable(Injectable<?> injectable) {
        Class<?> injectableClass = injectable.getClazz();
        if (allInjectedClasses.contains(injectableClass)) {
            LOG.warn("You should only inject a single instance for any given class. You have already injected class {}", injectableClass);
        } else {
            allInjectedClasses.add(injectableClass);
            allInjectables.add(injectable);
        }

        return this;
    }

    public JerseyEndpoints addContainerRequestFilter(ContainerRequestFilter containerRequestFilter) {
        containerRequestFilters.add(containerRequestFilter);
        return this;
    }

    public JerseyEndpoints addContainerResponseFilter(ContainerResponseFilter containerResponseFilter) {
        containerResponseFilters.add(containerResponseFilter);
        return this;
    }

    public JerseyEndpoints enableCORS() {
        supportCORS = true;
        return this;
    }

    public JerseyEndpoints humanReadableJson() {
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        return this;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    @Override
    public Handler getHandler(final Server server, String context, String applicationName) {
        ResourceConfig rc = new ResourceConfig();

        if (enableSwagger) {
            BeanConfig beanConfig = new BeanConfig();
            beanConfig.setVersion("1.0.0");
            beanConfig.setResourcePackage("");
            beanConfig.setScan(true);
            beanConfig.setBasePath("/");
            beanConfig.setTitle(applicationName);

            Set<String> packages = new HashSet<>();
            packages.add(ApiListingResource.class.getPackage().getName());
            for (Class<?> clazz : allClasses) {
                packages.add(clazz.getPackage().getName());
            }
            rc.packages(packages.toArray(new String[0]));
        }

        rc.registerClasses(allClasses);
        rc.register(HttpMethodOverrideFilter.class);
        rc.register(new JacksonFeature().withMapper(mapper));
        rc.register(MultiPartFeature.class); // adds support for multi-part API requests
        rc.registerInstances(allBinders);
        rc.registerInstances(
            new InjectableBinder(allInjectables),
            new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(server).to(Server.class);
                }
            }
        );

        if (supportCORS) {
            rc.register(CorsContainerResponseFilter.class);
        }

        for (ContainerRequestFilter containerRequestFilter : containerRequestFilters) {
            rc.register(containerRequestFilter);
        }

        for (ContainerResponseFilter containerResponseFilter : containerResponseFilters) {
            rc.register(containerResponseFilter);
        }


        ServletHolder servletHolder = new ServletHolder(new ServletContainer(rc));
        ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        servletContextHandler.setContextPath(context);
        if (!applicationName.isEmpty()) {
            servletContextHandler.setDisplayName(applicationName);
        }
        servletContextHandler.addServlet(servletHolder, "/*");
        servletContextHandler.addFilter(NewRelicRequestFilter.class, "/", EnumSet.of(DispatcherType.REQUEST));

        return servletContextHandler;
    }

}
