// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.oauth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import org.springframework.boot.ssl.SslBundle;

import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

final class CertificateClientAuthentication {
    private final String clientId;
    private final URI tokenEndpoint;
    private final RSAPrivateKey privateKey;
    private final X509Certificate certificate;
    private final Base64URL thumbprint;

    CertificateClientAuthentication(String clientId, URI tokenEndpoint, SslBundle bundle) {
        this.clientId = clientId;
        this.tokenEndpoint = tokenEndpoint;
        try {
            var stores = bundle.getStores();
            var keyStore = stores.getKeyStore();
            if (keyStore == null) {
                throw new IllegalArgumentException("Fluvius OAuth certificate bundle requires a keystore");
            }
            var alias = keyAlias(bundle, keyStore);
            var password = bundle.getKey().getPassword();
            if (password == null) {
                password = stores.getKeyStorePassword();
            }
            var key = keyStore.getKey(alias, password == null ? new char[0] : password.toCharArray());
            var cert = keyStore.getCertificate(alias);
            if (!(key instanceof RSAPrivateKey rsaPrivateKey) || !(cert instanceof X509Certificate x509Certificate)) {
                throw new IllegalArgumentException("Fluvius OAuth certificate bundle requires an RSA private key and X.509 certificate");
            }
            if (!(x509Certificate.getPublicKey() instanceof RSAPublicKey publicKey)
                    || !publicKey.getModulus().equals(rsaPrivateKey.getModulus())) {
                throw new IllegalArgumentException("Fluvius OAuth private key does not match its certificate");
            }
            x509Certificate.checkValidity();
            this.privateKey = rsaPrivateKey;
            this.certificate = x509Certificate;
            this.thumbprint = Base64URL.encode(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded()));
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Cannot load Fluvius OAuth certificate credentials", exception);
        }
    }

    PrivateKeyJWT create() throws GeneralSecurityException, JOSEException {
        certificate.checkValidity();
        var now = Instant.now();
        var claims = new JWTClaimsSet.Builder()
                .issuer(clientId)
                .subject(clientId)
                .audience(tokenEndpoint.toString())
                .claim(JWTClaimNames.ISSUED_AT, now.getEpochSecond())
                .claim(JWTClaimNames.NOT_BEFORE, now.getEpochSecond())
                .claim(JWTClaimNames.EXPIRATION_TIME, now.plusSeconds(300).getEpochSecond())
                .jwtID(UUID.randomUUID().toString())
                .build();
        var header = new JWSHeader.Builder(JWSAlgorithm.PS256)
                .type(JOSEObjectType.JWT)
                .x509CertSHA256Thumbprint(thumbprint)
                .build();
        var assertion = new SignedJWT(header, claims);
        assertion.sign(new RSASSASigner(privateKey));
        return new PrivateKeyJWT(assertion);
    }

    private static String keyAlias(SslBundle bundle, KeyStore keyStore) throws GeneralSecurityException {
        var configuredAlias = bundle.getKey().getAlias();
        if (configuredAlias != null) {
            return configuredAlias;
        }
        var aliases = new ArrayList<String>();
        var entries = keyStore.aliases();
        while (entries.hasMoreElements()) {
            var alias = entries.nextElement();
            if (keyStore.isKeyEntry(alias)) {
                aliases.add(alias);
            }
        }
        if (aliases.size() != 1) {
            throw new IllegalArgumentException("Fluvius OAuth certificate bundle must contain one private key or specify key.alias");
        }
        return aliases.getFirst();
    }
}
