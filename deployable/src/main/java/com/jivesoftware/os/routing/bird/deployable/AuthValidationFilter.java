package com.jivesoftware.os.routing.bird.deployable;

import com.google.common.collect.Lists;
import com.jivesoftware.os.routing.bird.server.oauth.OAuthEvaluator;
import com.jivesoftware.os.routing.bird.server.oauth.OAuthServiceLocatorShim;
import com.jivesoftware.os.routing.bird.server.oauth.route.RouteOAuthValidatorInitializer;
import com.jivesoftware.os.routing.bird.server.oauth.route.RouteOAuthValidatorInitializer.RouteOAuthValidatorConfig;
import com.jivesoftware.os.routing.bird.server.oauth.validator.AuthValidator;
import com.jivesoftware.os.routing.bird.server.session.RouteSessionValidatorInitializer;
import com.jivesoftware.os.routing.bird.server.session.RouteSessionValidatorInitializer.RouteSessionValidatorConfig;
import com.jivesoftware.os.routing.bird.server.session.SessionEvaluator;
import com.jivesoftware.os.routing.bird.server.session.SessionValidator;
import com.jivesoftware.os.routing.bird.shared.AuthEvaluator;
import com.jivesoftware.os.routing.bird.shared.AuthEvaluator.AuthStatus;
import com.jivesoftware.os.routing.bird.shared.TenantRoutingProvider;
import com.jivesoftware.os.routing.bird.shared.TenantsServiceConnectionDescriptorProvider;
import java.io.IOException;
import java.util.List;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.glassfish.jersey.oauth1.signature.OAuth1Request;
import org.glassfish.jersey.oauth1.signature.OAuth1Signature;

/**
 *
 */
public class AuthValidationFilter implements ContainerRequestFilter {

    private static final Response UNAUTHORIZED = Response.status(Status.UNAUTHORIZED).entity("Auth validation failed").build();

    private final Deployable deployable;
    private final InstanceConfig instanceConfig;

    private final List<PathedAuthEvaluator> evaluators = Lists.newArrayList();
    private final OAuth1Signature verifier = new OAuth1Signature(new OAuthServiceLocatorShim());

    public AuthValidationFilter(Deployable deployable) {
        this.deployable = deployable;
        this.instanceConfig = deployable.config(InstanceConfig.class);
    }

    public AuthValidationFilter addRouteOAuth(String... paths) throws Exception {
        TenantRoutingProvider tenantRoutingProvider = deployable.getTenantRoutingProvider();
        TenantsServiceConnectionDescriptorProvider connections = tenantRoutingProvider
            .getConnections(instanceConfig.getServiceName(), "main", 10_000); // TODO config

        RouteOAuthValidatorConfig routeOAuthValidatorConfig = deployable.config(RouteOAuthValidatorConfig.class);
        AuthValidator<OAuth1Signature, OAuth1Request> routeOAuthValidator = new RouteOAuthValidatorInitializer().initialize(routeOAuthValidatorConfig,
            connections,
            10_000);
        routeOAuthValidator.start();
        evaluators.add(new PathedAuthEvaluator(new OAuthEvaluator(routeOAuthValidator, verifier), paths));
        return this;
    }

    public AuthValidationFilter addCustomOAuth(AuthValidator<OAuth1Signature, OAuth1Request> customOAuthValidator, String... paths) {
        customOAuthValidator.start();
        evaluators.add(new PathedAuthEvaluator(new OAuthEvaluator(customOAuthValidator, verifier), paths));
        return this;
    }

    public AuthValidationFilter addSessionAuth(String... paths) throws Exception {
        RouteSessionValidatorConfig sessionValidatorConfig = deployable.config(RouteSessionValidatorConfig.class);
        SessionValidator routeSessionValidator = new RouteSessionValidatorInitializer().initialize(sessionValidatorConfig,
            instanceConfig.getRoutesHost(),
            instanceConfig.getRoutesPort(),
            "http", //TODO
            instanceConfig.getSessionValidatorPath());
        evaluators.add(new PathedAuthEvaluator(new SessionEvaluator(routeSessionValidator), paths));
        return this;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = '/' + requestContext.getUriInfo().getPath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        for (PathedAuthEvaluator pathedAuthEvaluator : evaluators) {
            if (pathedAuthEvaluator.matches(path)) {
                if (pathedAuthEvaluator.evaluator.authorize(requestContext) == AuthStatus.authorized) {
                    return;
                }
            }
        }
        requestContext.abortWith(UNAUTHORIZED);
    }

    public AuthValidationFilter addNoAuth(String... paths) {
        evaluators.add(new PathedAuthEvaluator(new NoAuthEvaluator(), paths));
        return this;
    }

    static class PathedAuthEvaluator {
        private final AuthEvaluator evaluator;
        private final String[] paths;
        private final boolean[] wildcards;

        /**
         * /a matches /a
         * /a/* matches /a, /a/, /a/b, but NOT /ab
         */
        PathedAuthEvaluator(AuthEvaluator evaluator, String... paths) {
            this.evaluator = evaluator;
            this.paths = new String[paths.length];
            this.wildcards = new boolean[paths.length];

            for (int i = 0; i < paths.length; i++) {
                boolean wildcard = paths[i].endsWith("/*");
                if (!wildcard && paths[i].contains("*")) {
                    throw new IllegalArgumentException("Wildcard paths must end in /*");
                }
                this.paths[i] = wildcard ? paths[i].substring(0, paths[i].length() - 2) : paths[i];
                this.wildcards[i] = wildcard;
            }
        }

        boolean matches(String path) {
            for (int i = 0; i < paths.length; i++) {
                if (wildcards[i]) {
                    if (path.startsWith(paths[i]) && (path.length() == paths[i].length() || path.charAt(paths[i].length()) == '/')) {
                        return true;
                    }
                } else if (path.equals(paths[i])) {
                    return true;
                }
            }
            return false;
        }
    }

    static class NoAuthEvaluator implements AuthEvaluator {

        @Override
        public AuthStatus authorize(ContainerRequestContext requestContext) throws IOException {
            return AuthStatus.authorized;
        }
    }
}
