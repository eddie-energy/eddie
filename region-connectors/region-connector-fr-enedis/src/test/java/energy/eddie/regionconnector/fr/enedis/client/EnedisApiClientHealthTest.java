package energy.eddie.regionconnector.fr.enedis.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.fr.enedis.FrEnedisSpringConfig;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    private final ObjectMapper objectMapper = new FrEnedisSpringConfig().objectMapper();

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
    void health_returnsUp_whenInitialized() {
        // Given
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        EnedisApiClient enedisApi = new EnedisApiClient(tokenProvider, webClient);

        // Then
        assertEquals(HealthState.UP, enedisApi.health().get(EnedisApiClient.AUTHENTICATION_API));
        assertEquals(HealthState.UP, enedisApi.health().get(EnedisApiClient.METERING_POINT_API));
        assertEquals(HealthState.UP, enedisApi.health().get(EnedisApiClient.CONTRACT_API));
    }

    @Test
    void health_returnsAUTHENTICATION_API_down_whenTokenFetchingFails() {
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
        assertEquals(HealthState.DOWN, enedisApi.health().get(EnedisApiClient.AUTHENTICATION_API));
        assertEquals(HealthState.UP, enedisApi.health().get(EnedisApiClient.METERING_POINT_API));
    }

    @Test
    void health_returnsMETERING_POINT_API_down_whenDataFetchingFails() {
        // Given
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        doReturn(Mono.just("token")).when(tokenProvider).getToken();
        EnedisApiClient enedisApi = new EnedisApiClient(tokenProvider, webClient);

        mockBackEnd.enqueue(new MockResponse().setResponseCode(500));

        // When
        enedisApi.getConsumptionMeterReading("usagePointId", LocalDate.now(ZoneOffset.UTC),
                                             LocalDate.now(ZoneOffset.UTC), Granularity.PT30M)
                 .as(StepVerifier::create)
                 .expectError()
                 .verify(Duration.ofSeconds(5));

        // Then
        assertEquals(HealthState.UP, enedisApi.health().get(EnedisApiClient.AUTHENTICATION_API));
        assertEquals(HealthState.DOWN, enedisApi.health().get(EnedisApiClient.METERING_POINT_API));
    }

    @Test
    void health_returnsCONTRACT_API_down_whenDataFetchingFails() {
        // Given
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        doReturn(Mono.just("token")).when(tokenProvider).getToken();
        EnedisApiClient enedisApi = new EnedisApiClient(tokenProvider, webClient);

        mockBackEnd.enqueue(new MockResponse().setResponseCode(500));

        // When
        enedisApi.getContract("usagePointId")
                 .as(StepVerifier::create)
                 .expectError()
                 .verify(Duration.ofSeconds(5));

        // Then
        assertEquals(HealthState.UP, enedisApi.health().get(EnedisApiClient.AUTHENTICATION_API));
        assertEquals(HealthState.DOWN, enedisApi.health().get(EnedisApiClient.CONTRACT_API));
    }
}
