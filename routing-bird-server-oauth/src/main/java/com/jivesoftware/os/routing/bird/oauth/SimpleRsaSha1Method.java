package com.jivesoftware.os.routing.bird.oauth;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import org.glassfish.jersey.oauth1.signature.Base64;
import org.glassfish.jersey.oauth1.signature.InvalidSecretException;
import org.glassfish.jersey.oauth1.signature.OAuth1Secrets;
import org.glassfish.jersey.oauth1.signature.OAuth1SignatureMethod;
import org.glassfish.jersey.oauth1.signature.internal.LocalizationMessages;

/**
 *
 * @author kevin.karpenske
 */
public class SimpleRsaSha1Method implements OAuth1SignatureMethod {

    private static final String NAME = "RSA-SHA1";

    private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";

    private static final String KEY_TYPE = "RSA";

    @Override
    public String name() {
        return NAME;
    }

    /**
     * Generates the RSA-SHA1 signature of OAuth request elements.
     *
     * @param baseString the combined OAuth elements to sign.
     * @param secrets    the secrets object containing the private key for generating the signature.
     * @return the OAuth signature, in base64-encoded form.
     * @throws InvalidSecretException if the supplied secret is not valid.
     */
    @Override
    public String sign(final String baseString, final OAuth1Secrets secrets) throws InvalidSecretException {

        byte[] decodedPrivateKey;
        try {
            decodedPrivateKey = Base64.decode(secrets.getConsumerSecret());
        } catch (IOException ioe) {
            throw new InvalidSecretException(LocalizationMessages.ERROR_INVALID_CONSUMER_SECRET(ioe));
        }

        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_TYPE);
            EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedPrivateKey);
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            signature.initSign(privateKey);
            signature.update(baseString.getBytes());
            byte[] rsasha1 = signature.sign();
            return Base64.encode(rsasha1);
        } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException | SignatureException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Verifies the RSA-SHA1 signature of OAuth request elements.
     *
     * @param elements  OAuth elements signature is to be verified against.
     * @param secrets   the secrets object containing the public key for verifying the signature.
     * @param signature base64-encoded OAuth signature to be verified.
     * @throws InvalidSecretException if the supplied secret is not valid.
     */
    @Override
    public boolean verify(final String elements, final OAuth1Secrets secrets, final String signature) throws InvalidSecretException {

        byte[] decodedSignature;
        try {
            decodedSignature = Base64.decode(signature);
        } catch (IOException e) {
            return false;
        }

        byte[] decodedPublicKey;
        try {
            decodedPublicKey = Base64.decode(secrets.getConsumerSecret());
        } catch (IOException ioe) {
            throw new InvalidSecretException(LocalizationMessages.ERROR_INVALID_CONSUMER_SECRET(ioe));
        }

        try {
            Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_TYPE);
            EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedPublicKey);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            sig.initVerify(publicKey);
            sig.update(elements.getBytes());
            return sig.verify(decodedSignature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException | SignatureException e) {
            throw new IllegalStateException(e);
        }
    }
}
