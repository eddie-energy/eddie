// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.oauth;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

final class CertificateTestFixture {
    static final String ALIAS = "test-certificate";
    static final String PASSWORD = "test-only-password";
    private final KeyStore keyStore;
    private final X509Certificate certificate;
    private final RSAPrivateKey privateKey;

    CertificateTestFixture() throws IOException, GeneralSecurityException {
        try (var input = new ClassPathResource("oauth/test-certificate.pem").getInputStream()) {
            certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(input);
        }
        var pem = new ClassPathResource("oauth/test-private-key.pem").getContentAsString(StandardCharsets.UTF_8)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "");
        privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA")
                                             .generatePrivate(new PKCS8EncodedKeySpec(Base64.getMimeDecoder().decode(pem)));
        keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        keyStore.setKeyEntry(ALIAS, privateKey, PASSWORD.toCharArray(), new java.security.cert.Certificate[]{certificate});
    }

    SslBundle bundle() {
        return bundle(SslBundleKey.of(null, ALIAS));
    }

    SslBundle bundle(SslBundleKey key) {
        return SslBundle.of(SslStoreBundle.of(keyStore, PASSWORD, null), key);
    }

    KeyStore keyStore() {
        return keyStore;
    }

    X509Certificate certificate() {
        return certificate;
    }

    RSAPrivateKey privateKey() {
        return privateKey;
    }
}
