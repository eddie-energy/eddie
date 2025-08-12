package energy.eddie.regionconnector.fr.enedis.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.fr.enedis.EnedisBeanConfig;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class EnedisApiClientHealthTest {
    private static MockWebServer mockBackEnd;
    private static WebClient webClient;
    private final ObjectMapper objectMapper = new EnedisBeanConfig().objectMapper();

    @BeforeEach
    void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        String basePath = "http://localhost:" + mockBackEnd.getPort();
        webClient = WebClient.builder()
                             .baseUrl(basePath)
                             .codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs()
                                                                                   .jackson2JsonDecoder(new Jackson2JsonDecoder(
                                                                                           objectMapper,
                                                                                           MediaType.APPLICATION_JSON)))
                             .build();
    }

    @Test
    void health_returnsUnknown_whenInitialized() {
        // Given
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        EnedisApiClient enedisApi = new EnedisApiClient(tokenProvider, webClient);

        // Then
        assertEquals(Status.UNKNOWN, enedisApi.health().get(EnedisApiClient.AUTHENTICATION_API).getStatus());
        assertEquals(Status.UNKNOWN, enedisApi.health().get(EnedisApiClient.METERING_POINT_API).getStatus());
        assertEquals(Status.UNKNOWN, enedisApi.health().get(EnedisApiClient.CONTRACT_API).getStatus());
        assertEquals(Status.UNKNOWN, enedisApi.health().get(EnedisApiClient.CONTACT_API).getStatus());
        assertEquals(Status.UNKNOWN, enedisApi.health().get(EnedisApiClient.IDENTITY_API).getStatus());
        assertEquals(Status.UNKNOWN, enedisApi.health().get(EnedisApiClient.ADDRESS_API).getStatus());
    }

    @Test
    void health_returnsAUTHENTICATION_API_unknown_whenTokenFetchingFails() {
        // Given
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        doReturn(Mono.error(WebClientResponseException.create(HttpStatus.UNAUTHORIZED.value(),
                                                              "xxx",
                                                              null,
                                                              null,
                                                              null))).when(tokenProvider).getToken();
        EnedisApiClient enedisApi = new EnedisApiClient(tokenProvider, webClient);

        // When
        enedisApi.getConsumptionMeterReading("usagePointId", LocalDate.now(ZoneOffset.UTC),
                                             LocalDate.now(ZoneOffset.UTC), Granularity.PT30M)
                 .as(StepVerifier::create)
                 .expectError()
                 .verify(Duration.ofSeconds(5));

        // Then
        assertEquals(Status.DOWN, enedisApi.health().get(EnedisApiClient.AUTHENTICATION_API).getStatus());
        assertEquals(Status.UNKNOWN, enedisApi.health().get(EnedisApiClient.METERING_POINT_API).getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {EnedisApiClient.CONTRACT_API, EnedisApiClient.CONTACT_API, EnedisApiClient.IDENTITY_API, EnedisApiClient.ADDRESS_API, EnedisApiClient.METERING_POINT_API})
    void health_returnsAPI_down_whenDataFetchingFails(String api) {
        // Given
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        doReturn(Mono.just("token")).when(tokenProvider).getToken();
        EnedisApiClient enedisApi = new EnedisApiClient(tokenProvider, webClient);

        mockBackEnd.enqueue(new MockResponse().setResponseCode(500));

        // When
        var apiResult = switch (api) {
            case EnedisApiClient.CONTRACT_API -> enedisApi.getContract("usagePointId");
            case EnedisApiClient.CONTACT_API -> enedisApi.getContact("usagePointId");
            case EnedisApiClient.IDENTITY_API -> enedisApi.getIdentity("usagePointId");
            case EnedisApiClient.ADDRESS_API -> enedisApi.getAddress("usagePointId");
            case EnedisApiClient.METERING_POINT_API -> enedisApi.getConsumptionMeterReading("usagePointId",
                                                                                            LocalDate.now(ZoneOffset.UTC),
                                                                                            LocalDate.now(ZoneOffset.UTC),
                                                                                            Granularity.PT30M);
            default -> throw new IllegalArgumentException("Unsupported API: " + api);
        };

        apiResult.as(StepVerifier::create)
                 .expectError()
                 .verify(Duration.ofSeconds(5));

        // Then
        assertEquals(Status.UP, enedisApi.health().get(EnedisApiClient.AUTHENTICATION_API).getStatus());
        assertEquals(Status.DOWN, enedisApi.health().get(api).getStatus());
    }
}
