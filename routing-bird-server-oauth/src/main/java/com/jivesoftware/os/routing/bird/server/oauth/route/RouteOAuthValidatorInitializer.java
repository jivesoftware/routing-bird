package com.jivesoftware.os.routing.bird.server.oauth.route;

import com.jivesoftware.os.routing.bird.server.oauth.OAuthPublicKeyProvider;
import com.jivesoftware.os.routing.bird.server.oauth.OAuthSecretManager;
import com.jivesoftware.os.routing.bird.server.oauth.validator.AuthValidator;
import com.jivesoftware.os.routing.bird.server.oauth.validator.DefaultOAuthValidator;
import com.jivesoftware.os.routing.bird.server.oauth.validator.NoOpAuthValidator;
import com.jivesoftware.os.routing.bird.shared.TenantsServiceConnectionDescriptorProvider;
import java.util.List;
import java.util.concurrent.Executors;
import jersey.repackaged.com.google.common.collect.Lists;
import org.glassfish.jersey.oauth1.signature.OAuth1Request;
import org.glassfish.jersey.oauth1.signature.OAuth1Signature;
import org.merlin.config.Config;
import org.merlin.config.defaults.BooleanDefault;
import org.merlin.config.defaults.IntDefault;
import org.merlin.config.defaults.LongDefault;

/**
 *
 */
public class RouteOAuthValidatorInitializer {

    public interface RouteOAuthValidatorConfig extends Config {

        @BooleanDefault(value = true)
        boolean getOauthValidatorIsEnabled();

        // Will expire cached secrets more than 60 seconds old ...
        @LongDefault(60 * 1000)
        long getOauthValidatorCheckForRemovedSecretsEveryNMillis();

        // Will reject requests more than five seconds old ...
        @LongDefault(5 * 1000)
        long getOauthValidatorRequestTimestampAgeLimitMillis();

        @LongDefault(72 * 60 * 60 * 1000)
        long getOauthValidatorSecretTimeoutHardMillis();

        @LongDefault(3 * 60 * 1000)
        long getOauthValidatorSecretTimeoutSoftMillis();

        @LongDefault(30 * 1000)
        long getOauthValidatorSecretCacheMissTimeoutMillis();

    }

    public AuthValidator<OAuth1Signature, OAuth1Request> initialize(RouteOAuthValidatorConfig config,
        TenantsServiceConnectionDescriptorProvider<String> connectionDescriptorProvider,
        long publicKeyExpirationMillis) throws Exception {

        if (!config.getOauthValidatorIsEnabled()) {
            return (AuthValidator) NoOpAuthValidator.SINGLETON;
        }

        List<OAuthPublicKeyProvider> oAuthPublicKeyProviders = Lists.newArrayList();
        oAuthPublicKeyProviders.add(new RouteOAuthPublicKeyProvider(connectionDescriptorProvider, publicKeyExpirationMillis));

        OAuthSecretManager authSecretManager = new OAuthSecretManager("routes",
            oAuthPublicKeyProviders,
            config.getOauthValidatorSecretTimeoutHardMillis(),
            config.getOauthValidatorSecretTimeoutSoftMillis(),
            config.getOauthValidatorSecretCacheMissTimeoutMillis());

        return new DefaultOAuthValidator(Executors.newScheduledThreadPool(1),
            config.getOauthValidatorCheckForRemovedSecretsEveryNMillis(),
            authSecretManager,
            config.getOauthValidatorRequestTimestampAgeLimitMillis(),
            false);
    }
}
