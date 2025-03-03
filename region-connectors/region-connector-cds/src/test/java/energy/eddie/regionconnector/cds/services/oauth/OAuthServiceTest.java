package energy.eddie.regionconnector.cds.services.oauth;

import energy.eddie.regionconnector.cds.client.Scopes;
import energy.eddie.regionconnector.cds.client.customer.data.CustomerDataClientCredentials;
import energy.eddie.regionconnector.cds.config.CdsConfiguration;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.master.data.CdsServerBuilder;
import energy.eddie.regionconnector.cds.services.oauth.par.ErrorParResponse;
import energy.eddie.regionconnector.cds.services.oauth.par.SuccessfulParResponse;
import energy.eddie.regionconnector.cds.services.oauth.par.UnableToSendPar;
import energy.eddie.regionconnector.cds.services.oauth.token.CredentialsWithRefreshToken;
import energy.eddie.regionconnector.cds.services.oauth.token.CredentialsWithoutRefreshToken;
import energy.eddie.regionconnector.cds.services.oauth.token.InvalidTokenResult;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.*;

class OAuthServiceTest {
    private static MockWebServer mockWebServer;
    private static CdsServer cdsServer;
    private final OAuthService oAuthService = new OAuthService(new CdsConfiguration(URI.create("http://localhost")));

    @BeforeAll
    static void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        cdsServer = new CdsServerBuilder().setBaseUri("http://localhost")
                                          .setName("CDS server")
                                          .setCoverages(Set.of())
                                          .setClientId("client-id")
                                          .setClientSecret("client-secret")
                                          .setTokenEndpoint(mockWebServer.url("/").toString())
                                          .setAuthorizationEndpoint("http://localhost")
                                          .setParEndpoint(mockWebServer.url("/").toString())
                                          .build();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testPushAuthorization_withValidResponse_returnsSuccessfulResponse() {
        // Given
        mockWebServer.enqueue(new MockResponse()
                                      .setResponseCode(200)
                                      .setBody("""
                                                       {
                                                        "request_uri":
                                                          "urn:ietf:params:oauth:request_uri:6esc_11ACC5bwc014ltc14eY22c",
                                                        "expires_in": 60
                                                       }
                                                       """)
                                      .addHeader("Content-Type", "application/json"));
        var expected = URI.create(
                "http://localhost?client_id=client-id&request_uri=urn%3Aietf%3Aparams%3Aoauth%3Arequest_uri%3A6esc_11ACC5bwc014ltc14eY22c");

        // When
        var res = oAuthService.pushAuthorization(cdsServer,
                                                 new CustomerDataClientCredentials("client-id"),
                                                 List.of(Scopes.USAGE_DETAILED_SCOPE));

        // Then
        var success = assertInstanceOf(SuccessfulParResponse.class, res);
        assertEquals(expected, success.redirectUri());
    }

    @Test
    void testPushAuthorization_withInvalidResponse_returnsErrorResponse() {
        // Given
        //noinspection JsonStandardCompliance
        mockWebServer.enqueue(new MockResponse()
                                      .setResponseCode(200)
                                      .setBody("INVALID RESPONSE")
                                      .addHeader("Content-Type", "application/json"));

        // When
        var res = oAuthService.pushAuthorization(cdsServer, new CustomerDataClientCredentials("client-id"), List.of(Scopes.USAGE_DETAILED_SCOPE));

        // Then
        assertInstanceOf(ErrorParResponse.class, res);
    }

    @Test
    void testPushAuthorization_withInvalidHttpResponse_returnsErrorResponse() {
        // Given
        mockWebServer.enqueue(new MockResponse().setStatus("INVALID STATUS LINE"));

        // When
        var res = oAuthService.pushAuthorization(cdsServer, new CustomerDataClientCredentials("client-id"), List.of(Scopes.USAGE_DETAILED_SCOPE));

        // Then
        assertInstanceOf(UnableToSendPar.class, res);
    }

    @Test
    void testPushAuthorization_withErrorResponse_returnsErrorResponse() {
        // Given
        mockWebServer.enqueue(new MockResponse()
                                      .setResponseCode(400)
                                      .setBody("{ \"error\": \"bla\" }")
                                      .addHeader("Content-Type", "application/json"));

        // When
        var res = oAuthService.pushAuthorization(cdsServer, new CustomerDataClientCredentials("client-id"), List.of(Scopes.USAGE_DETAILED_SCOPE));

        // Then
        var error = assertInstanceOf(ErrorParResponse.class, res);
        assertEquals("bla", error.code());
    }

    @Test
    void testRetrieveAccessToken_withInvalidServerResponse_returnsInvalidCodeResult() {
        // Given
        mockWebServer.enqueue(new MockResponse()
                                      .setStatus("Invalid status line"));

        // When
        var res = oAuthService.retrieveAccessToken("code", cdsServer, new CustomerDataClientCredentials("client-id"));

        // Then
        assertInstanceOf(InvalidTokenResult.class, res);
    }

    @Test
    void testRetrieveAccessToken_withErrorServerResponse_returnsInvalidCodeResult() {
        // Given
        mockWebServer.enqueue(new MockResponse()
                                      .setResponseCode(400)
                                      .addHeader("Content-Type", "application/json")
                                      .setBody("{ \"error\": \"bla\" }"));

        // When
        var res = oAuthService.retrieveAccessToken("code", cdsServer, new CustomerDataClientCredentials("client-id"));

        // Then
        assertInstanceOf(InvalidTokenResult.class, res);
    }

    @Test
    void testRetrieveAccessToken_withCredentialsResponse_returnsCredentials() {
        // Given
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Type", "application/json")
                        .setBody("""
                                         {
                                           "token_type": "Bearer",
                                           "access_token": "accessToken",
                                           "refresh_token": "refreshToken",
                                           "expires_in": 90
                                         }
                                         """)
        );

        // When
        var res = oAuthService.retrieveAccessToken("code", cdsServer, new CustomerDataClientCredentials("client-id"));

        // Then
        var creds = assertInstanceOf(CredentialsWithRefreshToken.class, res);
        var expiresAt = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(90);
        assertAll(
                () -> assertEquals("accessToken", creds.accessToken()),
                () -> assertEquals("refreshToken", creds.refreshToken()),
                () -> assertThat(creds.expiresAt()).isCloseTo(expiresAt, within(5, ChronoUnit.SECONDS))
        );
    }

    @Test
    void testRetrieveAccessToken_withAdminCredentials_returnsCredentials() {
        // Given
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Type", "application/json")
                        .setBody("""
                                         {
                                           "token_type": "Bearer",
                                           "access_token": "accessToken",
                                           "expires_in": 90
                                         }
                                         """)
        );

        // When
        var res = oAuthService.retrieveAccessToken(cdsServer);

        // Then
        var creds = assertInstanceOf(CredentialsWithoutRefreshToken.class, res);
        var expiresAt = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(90);
        assertAll(
                () -> assertEquals("accessToken", creds.accessToken()),
                () -> assertThat(creds.expiresAt()).isCloseTo(expiresAt, within(5, ChronoUnit.SECONDS))
        );
    }

    @Test
    void testCreateAuthorizationUri_returnsCorrectUri() {
        // Given
        var scopes = List.of(Scopes.USAGE_DETAILED_SCOPE);

        // When
        var res = oAuthService.createAuthorizationUri(cdsServer, new CustomerDataClientCredentials("client-id"), scopes);

        // Then
        var expected = "http://localhost?response_type=code&redirect_uri=http%3A%2F%2Flocalhost&state=" + res.state() + "&client_id=client-id&scope=cds_usage_detailed";
        assertEquals(expected, res.redirectUri().toString());
    }
}