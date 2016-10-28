package com.jivesoftware.os.routing.bird.deployable;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import oauth.signpost.OAuth;
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
    public String sign(HttpRequest request, HttpParameters requestParams)
        throws OAuthMessageSignerException {
        try {
            String keyString = OAuth.percentEncode(getConsumerSecret()) + '&'
                + OAuth.percentEncode(getTokenSecret());
            byte[] keyBytes = keyString.getBytes(OAuth.ENCODING);

            String sbs = new SignatureBaseString(request, requestParams).generate();
            OAuth.debugOut("SBS", sbs);

            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_TYPE);
            EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            signature.initSign(privateKey);
            signature.update(sbs.getBytes());
            byte[] rsasha1 = signature.sign();
            return base64Encode(rsasha1).trim();
        } catch (GeneralSecurityException e) {
            throw new OAuthMessageSignerException(e);
        } catch (UnsupportedEncodingException e) {
            throw new OAuthMessageSignerException(e);
        }
    }
}
