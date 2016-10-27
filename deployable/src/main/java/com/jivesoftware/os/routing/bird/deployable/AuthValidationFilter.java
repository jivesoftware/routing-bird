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
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
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

    private final List<AuthEvaluator> evaluators = Lists.newArrayList();
    private final OAuth1Signature verifier = new OAuth1Signature(new OAuthServiceLocatorShim());

    public AuthValidationFilter(Deployable deployable) {
        this.deployable = deployable;
        this.instanceConfig = deployable.config(InstanceConfig.class);
    }

    public AuthValidationFilter addRouteOAuth(String... paths) throws Exception {
        RouteOAuthValidatorConfig routeOAuthValidatorConfig = deployable.config(RouteOAuthValidatorConfig.class);
        AuthValidator<OAuth1Signature, OAuth1Request> routeOAuthValidator = new RouteOAuthValidatorInitializer().initialize(routeOAuthValidatorConfig,
            instanceConfig.getRoutesHost(),
            instanceConfig.getRoutesPort(),
            "http", //TODO
            instanceConfig.getAuthProviderKeyPath(),
            instanceConfig.getAuthProviderRemovalsPath());
        routeOAuthValidator.start();
        evaluators.add(new OAuthEvaluator(routeOAuthValidator, verifier, paths));
        return this;
    }

    public AuthValidationFilter addCustomOAuth(AuthValidator<OAuth1Signature, OAuth1Request> customOAuthValidator, String... paths) {
        customOAuthValidator.start();
        evaluators.add(new OAuthEvaluator(customOAuthValidator, verifier, paths));
        return this;
    }

    public AuthValidationFilter addSessionAuth(String... paths) throws Exception {
        RouteSessionValidatorConfig sessionValidatorConfig = deployable.config(RouteSessionValidatorConfig.class);
        SessionValidator routeSessionValidator = new RouteSessionValidatorInitializer().initialize(sessionValidatorConfig,
            instanceConfig.getRoutesHost(),
            instanceConfig.getRoutesPort(),
            "http", //TODO
            instanceConfig.getSessionValidatorPath());
        evaluators.add(new SessionEvaluator(routeSessionValidator, paths));
        return this;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        for (AuthEvaluator evaluator : evaluators) {
            AuthStatus status = evaluator.authorize(requestContext);
            if (status == AuthStatus.authorized) {
                return;
            }
        }
        requestContext.abortWith(UNAUTHORIZED);
    }

    public AuthValidationFilter addNoAuth(String... paths) {
        evaluators.add(new NoAuthEvaluator(paths));
        return this;
    }

    private static class NoAuthEvaluator implements AuthEvaluator {

        private final List<Pattern> patterns;

        private NoAuthEvaluator(String[] paths) {
            this.patterns = Lists.newArrayListWithCapacity(paths.length);

            for (String path : paths) {
                patterns.add(Pattern.compile(path));
            }
        }

        @Override
        public AuthStatus authorize(ContainerRequestContext requestContext) throws IOException {
            for (Pattern pattern : patterns) {
                String path = '/' + requestContext.getUriInfo().getPath();
                if (pattern.matcher(path).matches()) {
                    return AuthStatus.authorized;
                }
            }
            return AuthStatus.not_handled;
        }
    }
}
