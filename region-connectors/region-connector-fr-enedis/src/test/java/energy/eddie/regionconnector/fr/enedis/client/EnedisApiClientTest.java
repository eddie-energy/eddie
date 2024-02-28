package energy.eddie.regionconnector.fr.enedis.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.fr.enedis.TestResourceProvider;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@SuppressWarnings("OptionalGetWithoutIsPresent")
class EnedisApiClientTest {
    private static MockWebServer mockBackEnd;
    private static WebClient webClient;

    @BeforeEach
    void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        String basePath = "http://localhost:" + mockBackEnd.getPort();
        webClient = WebClient.builder()
                .baseUrl(basePath)
                .codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs()
                        .jackson2JsonDecoder(new Jackson2JsonDecoder(
                                new ObjectMapper()
                                        .registerModule(new JavaTimeModule())
                                        .registerModule(new Jdk8Module()),
                                MediaType.APPLICATION_JSON)))
                .build();
    }

    @Test
    void getConsumptionMeterReading_1Day_PT30M_returnsConsumption() throws IOException {
        // Arrange
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        doReturn(Mono.just("token")).when(tokenProvider).getToken();
        EnedisApi enedisApi = new EnedisApiClient(tokenProvider, webClient);

        mockBackEnd.enqueue(TestResourceProvider.readMockResponseFromFile(TestResourceProvider.CONSUMPTION_LOAD_CURVE_1_DAY));
        String usagePointId = "24115050XXXXXX";
        LocalDate start = LocalDate.of(2024, 2, 26);
        LocalDate end = LocalDate.of(2024, 2, 27);

        // Act & Assert
        enedisApi.getConsumptionMeterReading(usagePointId, start, end, Granularity.PT30M)
                .as(StepVerifier::create)
                .assertNext(consumption -> {
                    // The API for some reason returns 47 readings instead of 48
                    assertEquals(47, consumption.intervalReadings().size());
                    assertEquals(usagePointId, consumption.usagePointId());
                    assertEquals(start, consumption.start());
                    assertEquals(end, consumption.end());
                    assertEquals(Granularity.PT30M.toString(), consumption.intervalReadings().getFirst().intervalLength().get());
                })
                .expectComplete()
                .verify(Duration.ofSeconds(5));
    }

    @Test
    void getConsumptionMeterReading_7Days_PT1D_returnsConsumption() throws IOException {
        // Arrange
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        doReturn(Mono.just("token")).when(tokenProvider).getToken();
        EnedisApi enedisApi = new EnedisApiClient(tokenProvider, webClient);

        mockBackEnd.enqueue(TestResourceProvider.readMockResponseFromFile(TestResourceProvider.DAILY_CONSUMPTION_1_WEEK));
        String usagePointId = "24115050XXXXXX";
        LocalDate start = LocalDate.of(2024, 2, 1);
        LocalDate end = LocalDate.of(2024, 2, 8);
        // Act & Assert
        enedisApi.getConsumptionMeterReading(usagePointId, start, end, Granularity.P1D)
                .as(StepVerifier::create)
                .assertNext(consumption -> {
                    assertEquals(7, consumption.intervalReadings().size());
                    assertEquals(usagePointId, consumption.usagePointId());
                    assertEquals(start, consumption.start());
                    assertEquals(end, consumption.end());
                    assertEquals(Granularity.P1D.toString(), consumption.readingType().measuringPeriod().get());
                })
                .expectComplete()
                .verify(Duration.ofSeconds(5));
    }

    @Test
    void getConsumptionMeterReading_throwsIllegalArgumentException_withUnsupportedGranularity() {
        // Arrange
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        doReturn(Mono.just("token")).when(tokenProvider).getToken();
        EnedisApi enedisApi = new EnedisApiClient(tokenProvider, webClient);

        // Act & Assert
        enedisApi.getConsumptionMeterReading("usagePointId", LocalDate.now(), LocalDate.now(), Granularity.PT15M)
                .as(StepVerifier::create)
                .expectError(IllegalArgumentException.class)
                .verify(Duration.ofSeconds(5));
    }

    @Test
    void health_returnsUp_whenInitialized() {
        // Arrange
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        EnedisApi enedisApi = new EnedisApiClient(tokenProvider, webClient);

        // Assert
        assertEquals(HealthState.UP, enedisApi.health().get(EnedisApiClient.AUTHENTICATION_API));
        assertEquals(HealthState.UP, enedisApi.health().get(EnedisApiClient.METERING_POINT_API));
    }

    @Test
    void health_returnsAUTHENTICATION_API_down_whenTokenFetchingFails() {
        // Arrange
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        doReturn(Mono.error(WebClientResponseException.create(HttpStatus.UNAUTHORIZED.value(), "xxx", null, null, null))).when(tokenProvider).getToken();
        EnedisApi enedisApi = new EnedisApiClient(tokenProvider, webClient);

        // Act
        enedisApi.getConsumptionMeterReading("usagePointId", LocalDate.now(), LocalDate.now(), Granularity.PT30M)
                .as(StepVerifier::create)
                .expectError()
                .verify(Duration.ofSeconds(5));

        // Assert
        assertEquals(HealthState.DOWN, enedisApi.health().get(EnedisApiClient.AUTHENTICATION_API));
        assertEquals(HealthState.UP, enedisApi.health().get(EnedisApiClient.METERING_POINT_API));
    }

    @Test
    void health_returnsMETERING_POINT_API_down_whenDataFetchingFails() {
        // Arrange
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        doReturn(Mono.just("token")).when(tokenProvider).getToken();
        EnedisApi enedisApi = new EnedisApiClient(tokenProvider, webClient);

        mockBackEnd.enqueue(new MockResponse().setResponseCode(500));

        // Act
        enedisApi.getConsumptionMeterReading("usagePointId", LocalDate.now(), LocalDate.now(), Granularity.PT30M)
                .as(StepVerifier::create)
                .expectError()
                .verify(Duration.ofSeconds(5));

        // Assert
        assertEquals(HealthState.UP, enedisApi.health().get(EnedisApiClient.AUTHENTICATION_API));
        assertEquals(HealthState.DOWN, enedisApi.health().get(EnedisApiClient.METERING_POINT_API));
    }
}