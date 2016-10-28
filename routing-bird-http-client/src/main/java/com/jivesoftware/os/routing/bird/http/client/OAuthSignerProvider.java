package com.jivesoftware.os.routing.bird.http.client;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public class OAuthSignerProvider {

    private final Callable<OAuthSigner> signerProvider;
    private final AtomicReference<OAuthSigner> signerRef = new AtomicReference<>();

    public OAuthSignerProvider(Callable<OAuthSigner> signerProvider) {
        this.signerProvider = signerProvider;
    }

    public OAuthSigner get() {
        return signerRef.updateAndGet(oAuthSigner -> {
            try {
                if (oAuthSigner == null) {
                    oAuthSigner = signerProvider.call();
                }
                return oAuthSigner;
            } catch (Exception e) {
                throw new RuntimeException("OAuthSigner is unavailable", e);
            }
        });
    }
}
