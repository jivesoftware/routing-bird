package com.jivesoftware.os.routing.bird.deployable;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.http.HttpParameters;
import oauth.signpost.http.HttpRequest;
import oauth.signpost.signature.OAuthMessageSigner;
import oauth.signpost.signature.SignatureBaseString;
import org.glassfish.jersey.oauth1.signature.Base64;

/**
 *
 */
public class RsaSha1MessageSigner extends OAuthMessageSigner {

    private static final String NAME = "RSA-SHA1";

    private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";

    private static final String KEY_TYPE = "RSA";

    @Override
    public String getSignatureMethod() {
        return NAME;
    }

    @Override
    public String sign(HttpRequest request, HttpParameters requestParams) throws OAuthMessageSignerException {
        byte[] decodedPrivateKey;
        try {
            decodedPrivateKey = Base64.decode(getConsumerSecret());
            String baseString = new SignatureBaseString(request, requestParams).generate();
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_TYPE);
            EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedPrivateKey);
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            signature.initSign(privateKey);
            signature.update(baseString.getBytes());
            byte[] rsasha1 = signature.sign();
            return Base64.encode(rsasha1);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException | SignatureException e) {
            throw new OAuthMessageSignerException(e);
        }
    }
}
