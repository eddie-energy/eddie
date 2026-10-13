// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.SslStoreBundle;

import java.net.URI;
import java.security.KeyPairGenerator;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateClientAuthenticationTest {
    private static final URI TOKEN_ENDPOINT = URI.create("https://login.microsoftonline.com/test/oauth2/v2.0/token");
    private CertificateTestFixture fixture;
    @Mock
    private X509Certificate expiredCertificate;

    @BeforeEach
    void setUp() throws Exception {
        fixture = new CertificateTestFixture();
    }

    @Test
    void automaticallySelectsTheOnlyPrivateKey() throws Exception {
        fixture.keyStore().setCertificateEntry("trust-certificate", fixture.certificate());
        var authentication = new CertificateClientAuthentication("client", TOKEN_ENDPOINT, fixture.bundle(SslBundleKey.NONE));
        assertNotNull(authentication.create());
    }

    @Test
    void requiresKeystore() {
        var bundle = SslBundle.of(null);
        var error = assertThrows(IllegalArgumentException.class,
                                 () -> new CertificateClientAuthentication("client", TOKEN_ENDPOINT, bundle));
        assertTrue(error.getMessage().contains("requires a keystore"));
    }

    @Test
    void requiresAnUnambiguousPrivateKey() throws Exception {
        fixture.keyStore().setKeyEntry("second", fixture.privateKey(), CertificateTestFixture.PASSWORD.toCharArray(),
                                      new Certificate[]{fixture.certificate()});
        var bundle = fixture.bundle(SslBundleKey.NONE);
        assertThrows(IllegalArgumentException.class,
                     () -> new CertificateClientAuthentication("client", TOKEN_ENDPOINT, bundle));
        assertNotNull(new CertificateClientAuthentication("client", TOKEN_ENDPOINT, fixture.bundle()).create());
    }

    @Test
    void rejectsKeystoreWithoutPrivateKey() throws Exception {
        fixture.keyStore().deleteEntry(CertificateTestFixture.ALIAS);
        fixture.keyStore().setCertificateEntry("certificate-only", fixture.certificate());
        var bundle = fixture.bundle(SslBundleKey.NONE);
        assertThrows(IllegalArgumentException.class,
                     () -> new CertificateClientAuthentication("client", TOKEN_ENDPOINT, bundle));
    }

    @Test
    void rejectsMissingAlias() {
        var bundle = fixture.bundle(SslBundleKey.of(null, "missing"));
        assertThrows(IllegalArgumentException.class,
                     () -> new CertificateClientAuthentication("client", TOKEN_ENDPOINT, bundle));
    }

    @Test
    void rejectsIncorrectKeyPassword() {
        var bundle = fixture.bundle(SslBundleKey.of("incorrect", CertificateTestFixture.ALIAS));
        var error = assertThrows(IllegalArgumentException.class,
                                 () -> new CertificateClientAuthentication("client", TOKEN_ENDPOINT, bundle));
        assertNotNull(error.getCause());
    }

    @Test
    void supportsBundleKeyPassword() throws Exception {
        var bundle = fixture.bundle(SslBundleKey.of(CertificateTestFixture.PASSWORD, CertificateTestFixture.ALIAS));
        assertNotNull(new CertificateClientAuthentication("client", TOKEN_ENDPOINT, bundle).create());
    }

    @Test
    void supportsUnencryptedPrivateKey() throws Exception {
        fixture.keyStore().setKeyEntry(CertificateTestFixture.ALIAS, fixture.privateKey(), new char[0],
                                      new Certificate[]{fixture.certificate()});
        var bundle = SslBundle.of(SslStoreBundle.of(fixture.keyStore(), null, null),
                                  SslBundleKey.of(null, CertificateTestFixture.ALIAS));
        assertNotNull(new CertificateClientAuthentication("client", TOKEN_ENDPOINT, bundle).create());
    }

    @Test
    void rejectsNonRsaPrivateKey() throws Exception {
        var key = KeyPairGenerator.getInstance("EC").generateKeyPair().getPrivate();
        fixture.keyStore().setKeyEntry(CertificateTestFixture.ALIAS, key, CertificateTestFixture.PASSWORD.toCharArray(),
                                      new Certificate[]{fixture.certificate()});
        var bundle = fixture.bundle();
        assertThrows(IllegalArgumentException.class,
                     () -> new CertificateClientAuthentication("client", TOKEN_ENDPOINT, bundle));
    }

    @Test
    void rejectsMismatchedCertificateAndPrivateKey() throws Exception {
        var key = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate();
        fixture.keyStore().setKeyEntry(CertificateTestFixture.ALIAS, key, CertificateTestFixture.PASSWORD.toCharArray(),
                                      new Certificate[]{fixture.certificate()});
        var bundle = fixture.bundle();
        var error = assertThrows(IllegalArgumentException.class,
                                 () -> new CertificateClientAuthentication("client", TOKEN_ENDPOINT, bundle));
        assertTrue(error.getMessage().contains("does not match"));
    }

    @Test
    void rejectsExpiredCertificateAtStartup() throws Exception {
        when(expiredCertificate.getPublicKey()).thenReturn(fixture.certificate().getPublicKey());
        doThrow(new CertificateExpiredException("expired")).when(expiredCertificate).checkValidity();
        fixture.keyStore().setKeyEntry(CertificateTestFixture.ALIAS, fixture.privateKey(), CertificateTestFixture.PASSWORD.toCharArray(),
                                      new Certificate[]{expiredCertificate});
        var bundle = fixture.bundle();
        var error = assertThrows(IllegalArgumentException.class,
                                 () -> new CertificateClientAuthentication("client", TOKEN_ENDPOINT, bundle));
        assertInstanceOf(CertificateExpiredException.class, error.getCause());
    }
}
