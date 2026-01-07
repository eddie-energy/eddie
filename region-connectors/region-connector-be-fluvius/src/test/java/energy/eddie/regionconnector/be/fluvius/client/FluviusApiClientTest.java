package energy.eddie.regionconnector.be.fluvius.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.ParseException;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.be.fluvius.client.model.*;
import energy.eddie.regionconnector.be.fluvius.config.FluviusConfiguration;
import energy.eddie.regionconnector.be.fluvius.oauth.OAuthException;
import energy.eddie.regionconnector.be.fluvius.oauth.OAuthRequestException;
import energy.eddie.regionconnector.be.fluvius.oauth.OAuthTokenService;
import energy.eddie.regionconnector.be.fluvius.permission.request.Flow;
import energy.eddie.regionconnector.shared.utils.ObjectMapperConfig;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FluviusApiClientTest {
    private static final String PUBLIC_URL = "https://localhost:8080";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperConfig().objectMapper();
    private static final MockWebServer SERVER = new MockWebServer();
    private static WebClient webClient;
    @Mock
    private OAuthTokenService oAuthTokenService;

    @BeforeAll
    static void setUp() throws IOException {
        SERVER.start();
        String basePath = "http://localhost:" + SERVER.getPort();
        webClient = WebClient.builder()
                             .baseUrl(basePath)
                             .codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs()
                                                                                   .jackson2JsonDecoder(new Jackson2JsonDecoder(
                                                                                           OBJECT_MAPPER,
                                                                                           MediaType.APPLICATION_JSON)))
                             .build();
    }


    @Test
    void testShortUrlIdentifier_returnsSuccessMessage() throws IOException, OAuthException, URISyntaxException, ParseException {
        // Given
        SERVER.enqueue(new MockResponse()
                               .addHeader("Content-Type", "application/json")
                               .setResponseCode(200)
                               .setBody(OBJECT_MAPPER.writeValueAsString(new FluviusSessionCreateResultResponseModelApiDataResponse(
                                       null,
                                       null))));
        when(oAuthTokenService.accessToken()).thenReturn("token");
        var api = new FluviusApiClient(webClient, getConfiguration(), oAuthTokenService, PUBLIC_URL);
        var now = ZonedDateTime.now(ZoneOffset.UTC);

        // When
        var res = api.shortUrlIdentifier("pid", Flow.B2B, now, now, Granularity.PT15M);

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void testShortUrlIdentifier_returnsError_onInvalidAccessToken() throws IOException, OAuthException, URISyntaxException, ParseException {
        // Given
        when(oAuthTokenService.accessToken()).thenThrow(OAuthException.class);
        var api = new FluviusApiClient(webClient, getConfiguration(), oAuthTokenService, PUBLIC_URL);
        var now = ZonedDateTime.now(ZoneOffset.UTC);

        // When
        var res = api.shortUrlIdentifier("pid", Flow.B2B, now, now, Granularity.PT15M);

        // Then
        StepVerifier.create(res)
                    .expectError(OAuthRequestException.class)
                    .verify();
    }

    @Test
    void testMandateFor_returnsError_onInvalidAccessToken() throws IOException, OAuthException, URISyntaxException, ParseException {
        // Given
        when(oAuthTokenService.accessToken()).thenThrow(OAuthException.class);
        var api = new FluviusApiClient(webClient, getConfiguration(), oAuthTokenService, PUBLIC_URL);

        // When
        var res = api.mandateFor("pid");

        // Then
        StepVerifier.create(res)
                    .expectError(OAuthRequestException.class)
                    .verify();
    }

    @Test
    void testMandateFor_returnsMandates_forPermissionId() throws IOException, OAuthException, URISyntaxException, ParseException {
        // Given
        when(oAuthTokenService.accessToken()).thenReturn("token");
        var api = new FluviusApiClient(webClient, getConfiguration(), oAuthTokenService, PUBLIC_URL);
        var response = new GetMandateResponseModelApiDataResponse(null, null);
        SERVER.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(OBJECT_MAPPER.writeValueAsString(response)));

        // When
        var res = api.mandateFor("pid");

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void testMockMandates_returnsSuccessMessage() throws IOException, OAuthException, URISyntaxException, ParseException {
        // Given
        var payload = new CreateMandateResponseModelApiDataResponse(null, null);
        SERVER.enqueue(new MockResponse()
                               .addHeader("Content-Type", "application/json")
                               .setResponseCode(200)
                               .setBody(OBJECT_MAPPER.writeValueAsString(payload))
        );
        when(oAuthTokenService.accessToken()).thenReturn("token");
        var api = new FluviusApiClient(webClient, getConfiguration(), oAuthTokenService, PUBLIC_URL);
        var now = ZonedDateTime.now(ZoneOffset.UTC);

        // When
        var res = api.mockMandate("pid", now, now, "541440110000000011", Granularity.PT15M);

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void testMockMandates_returnsError() throws IOException, OAuthException, URISyntaxException, ParseException {
        // Given
        SERVER.enqueue(new MockResponse()
                               .addHeader("Content-Type", "application/json")
                               .setResponseCode(400)
        );
        when(oAuthTokenService.accessToken()).thenReturn("token");
        var api = new FluviusApiClient(webClient, getConfiguration(), oAuthTokenService, PUBLIC_URL);
        var now = ZonedDateTime.now(ZoneOffset.UTC);

        // When
        var res = api.mockMandate("pid", now, now, "541440110000000011", Granularity.PT15M);

        // Then
        StepVerifier.create(res)
                    .expectError(WebClientResponseException.class)
                    .verify();
    }

    @Test
    void testEnergy_returnsSuccessMessage() throws IOException, OAuthException, URISyntaxException, ParseException {
        // Given
        SERVER.enqueue(new MockResponse()
                               .addHeader("Content-Type", "application/json")
                               .setResponseCode(200)
                               .setBody(OBJECT_MAPPER.writeValueAsString(new GetEnergyResponseModelApiDataResponse(
                                       new ApiMetaData(null),
                                       new GetEnergyResponseModel(null, null, null)
                               ))));
        when(oAuthTokenService.accessToken()).thenReturn("token");
        var api = new FluviusApiClient(webClient, getConfiguration(), oAuthTokenService, PUBLIC_URL);
        var now = ZonedDateTime.now(ZoneOffset.UTC);

        // When
        var res = api.energy("pid", "eanNumber", DataServiceType.DAILY, now, now);

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void testEnergy_returnsError_onInvalidAccessToken() throws IOException, OAuthException, URISyntaxException, ParseException {
        // Given
        when(oAuthTokenService.accessToken()).thenThrow(OAuthException.class);
        var api = new FluviusApiClient(webClient, getConfiguration(), oAuthTokenService, PUBLIC_URL);
        var now = ZonedDateTime.now(ZoneOffset.UTC);

        // When
        var res = api.energy("pid", "eanNumber", DataServiceType.DAILY, now, now);

        // Then
        StepVerifier.create(res)
                    .expectError(OAuthRequestException.class)
                    .verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 408})
    void testEnergy_updatesHealth_onServerErrorAndTimeout(int responseCode) throws OAuthException, URISyntaxException, IOException, ParseException {
        // Given
        SERVER.enqueue(new MockResponse().setResponseCode(responseCode));
        when(oAuthTokenService.accessToken()).thenReturn("token");
        var api = new FluviusApiClient(webClient, getConfiguration(), oAuthTokenService, PUBLIC_URL);
        var now = ZonedDateTime.now(ZoneOffset.UTC);

        // When
        var res = api.energy("pid", "eanNumber", DataServiceType.DAILY, now, now);

        // Then
        Assertions.assertEquals(Status.UNKNOWN, api.health().getStatus());
        StepVerifier.create(res)
                    .expectError()
                    .verify();
        Assertions.assertEquals(Status.DOWN, api.health().getStatus());
    }

    private static FluviusConfiguration getConfiguration() {
        return new FluviusConfiguration("https://localhost",
                                        "sub-key",
                                        "contract-number",
                                        false);
    }
}