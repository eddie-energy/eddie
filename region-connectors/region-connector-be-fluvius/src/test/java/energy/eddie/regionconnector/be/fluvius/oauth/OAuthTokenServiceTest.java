// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.oauth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.SignedJWT;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;

import java.security.MessageDigest;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration.AuthenticationMethod.CERTIFICATE;
import static org.junit.jupiter.api.Assertions.*;

class OAuthTokenServiceTest {
    private final MockWebServer server = new MockWebServer();
    private final DefaultSslBundleRegistry bundles = new DefaultSslBundleRegistry();
    private FluviusOAuthConfiguration config;

    @BeforeEach
    void setUp() throws Exception {
        server.start();
        config = new FluviusOAuthConfiguration(server.url("/token").toString(), "client-id", "client-secret", "tenant-id", "api://test/.default");
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void clientSecretRequestUsesBasicAuthenticationAndCachesOpaqueToken() throws Exception {
        server.enqueue(tokenResponse("opaque-access-token", 3600));
        var service = new OAuthTokenService(config, bundles);

        assertEquals("opaque-access-token", service.accessToken());
        assertEquals("opaque-access-token", service.accessToken());
        assertEquals(1, server.getRequestCount());
        var request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("Basic Y2xpZW50LWlkOmNsaWVudC1zZWNyZXQ=", request.getHeader("Authorization"));
        var body = request.getBody().readUtf8();
        assertTrue(body.contains("grant_type=client_credentials"));
        assertTrue(body.contains("scope=api%3A%2F%2Ftest%2F.default"));
        assertFalse(body.contains("client_assertion"));
    }

    @Test
    void tokenExpiringWithinOneMinuteIsRenewed() throws Exception {
        server.enqueue(tokenResponse("old-token", 30));
        server.enqueue(tokenResponse("new-token", 3600));
        var service = new OAuthTokenService(config, bundles);

        assertEquals("old-token", service.accessToken());
        assertEquals("new-token", service.accessToken());
        assertEquals(2, server.getRequestCount());
    }

    @Test
    void failedTokenRequestIsNotCached() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(400).addHeader("Content-Type", "application/json")
                                       .setBody("{\"error\":\"invalid_client\"}"));
        server.enqueue(tokenResponse("recovered-token", 3600));
        var service = new OAuthTokenService(config, bundles);

        assertThrows(OAuthException.class, service::accessToken);
        assertEquals("recovered-token", service.accessToken());
    }

    @Test
    void certificateRequestContainsSignedAssertionAndNoSecret() throws Exception {
        var fixture = new CertificateTestFixture();
        bundles.registerBundle("fluvius", fixture.bundle());
        config = new FluviusOAuthConfiguration(server.url("/token").toString(), "live-client", null,
                                               "tenant-id", "api://test/.default", CERTIFICATE, "fluvius");
        var service = new OAuthTokenService(config, bundles);
        server.enqueue(tokenResponse("live-token", 3600));

        assertEquals("live-token", service.accessToken());
        var request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertNull(request.getHeader("Authorization"));
        var parameters = com.nimbusds.oauth2.sdk.util.URLUtils.parseParameters(request.getBody().readUtf8());
        assertEquals("live-client", parameters.get("client_id").getFirst());
        assertEquals("urn:ietf:params:oauth:client-assertion-type:jwt-bearer", parameters.get("client_assertion_type").getFirst());
        assertFalse(parameters.containsKey("client_secret"));
        var assertion = SignedJWT.parse(parameters.get("client_assertion").getFirst());
        assertTrue(assertion.verify(new RSASSAVerifier((RSAPublicKey) fixture.certificate().getPublicKey())));
        assertEquals(JWSAlgorithm.PS256, assertion.getHeader().getAlgorithm());
        assertEquals(Base64URL.encode(MessageDigest.getInstance("SHA-256").digest(fixture.certificate().getEncoded())),
                     assertion.getHeader().getX509CertSHA256Thumbprint());
        var claims = assertion.getJWTClaimsSet();
        assertEquals("live-client", claims.getIssuer());
        assertEquals("live-client", claims.getSubject());
        assertEquals(config.tokenUrl(), claims.getAudience().getFirst());
        assertNotNull(claims.getJWTID());
        assertNotNull(claims.getIssueTime());
        assertNotNull(claims.getNotBeforeTime());
        assertTrue(claims.getExpirationTime().toInstant().isAfter(Instant.now()));
        assertEquals(300, claims.getExpirationTime().toInstant().getEpochSecond() - claims.getIssueTime().toInstant().getEpochSecond());
    }

    @Test
    void certificateRenewalCreatesFreshAssertion() throws Exception {
        bundles.registerBundle("fluvius", new CertificateTestFixture().bundle());
        config = new FluviusOAuthConfiguration(server.url("/token").toString(), "live-client", null,
                                               "tenant-id", "scope", CERTIFICATE, "fluvius");
        server.enqueue(tokenResponse("first", 0));
        server.enqueue(tokenResponse("second", 3600));
        var service = new OAuthTokenService(config, bundles);

        service.accessToken();
        service.accessToken();
        var first = server.takeRequest(1, TimeUnit.SECONDS);
        var second = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(first);
        assertNotNull(second);
        assertNotEquals(first.getBody().readUtf8(), second.getBody().readUtf8());
    }

    @Test
    void missingCertificateBundleFailsAtStartup() {
        var certificateConfig = new FluviusOAuthConfiguration(config.tokenUrl(), "live-client", null,
                                                              "tenant", "scope", CERTIFICATE, "missing");
        assertThrows(org.springframework.boot.ssl.NoSuchSslBundleException.class,
                     () -> new OAuthTokenService(certificateConfig, bundles));
    }

    private static MockResponse tokenResponse(String token, int expiresIn) {
        return new MockResponse().addHeader("Content-Type", "application/json")
                                 .setBody("{\"token_type\":\"Bearer\",\"expires_in\":" + expiresIn + ",\"access_token\":\"" + token + "\"}");
    }
}
