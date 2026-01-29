package energy.eddie.regionconnector.fr.enedis.client;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.fr.enedis.TestResourceProvider;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@SuppressWarnings("OptionalGetWithoutIsPresent")
class EnedisApiClientMeterReadingTest {
    private static MockWebServer mockBackEnd;
    private static WebClient webClient;

    @BeforeEach
    void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        String basePath = "http://localhost:" + mockBackEnd.getPort();
        webClient = WebClient.builder()
                .baseUrl(basePath)
                .build();
    }

    @Test
    void getConsumptionMeterReading_1Day_PT30M_returnsConsumption() throws IOException {
        // Given
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        doReturn(Mono.just("token")).when(tokenProvider).getToken();
        EnedisApiClient enedisApi = new EnedisApiClient(tokenProvider, webClient);

        mockBackEnd.enqueue(TestResourceProvider.readMockResponseFromFile(TestResourceProvider.CONSUMPTION_LOAD_CURVE_1_DAY));
        String usagePointId = "24115050XXXXXX";
        LocalDate start = LocalDate.of(2024, 2, 26);
        LocalDate end = LocalDate.of(2024, 2, 27);

        // When
        var res = enedisApi.getConsumptionMeterReading(usagePointId, start, end, Granularity.PT30M);

        // Then

        StepVerifier.create(res)
                .assertNext(consumption -> {
                    // The API for some reason returns 47 readings instead of 48
                    assertEquals(47, consumption.intervalReadings().size());
                    assertEquals(usagePointId, consumption.usagePointId());
                    assertEquals(start, consumption.start());
                    assertEquals(end, consumption.end());
                    assertEquals(Granularity.PT30M.toString(),
                            consumption.intervalReadings().getFirst().intervalLength().get());
                })
                .expectComplete()
                .verify(Duration.ofSeconds(5));
    }

    @Test
    void getConsumptionMeterReading_7Days_PT1D_returnsConsumption() throws IOException {
        // Given
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        doReturn(Mono.just("token")).when(tokenProvider).getToken();
        EnedisApiClient enedisApi = new EnedisApiClient(tokenProvider, webClient);

        mockBackEnd.enqueue(TestResourceProvider.readMockResponseFromFile(TestResourceProvider.DAILY_CONSUMPTION_1_WEEK));
        String usagePointId = "24115050XXXXXX";
        LocalDate start = LocalDate.of(2024, 2, 1);
        LocalDate end = LocalDate.of(2024, 2, 8);
        // When & Then
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
    void getProductionMeterReading_1Day_PT30M_returnsConsumption() throws IOException {
        // Given
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        doReturn(Mono.just("token")).when(tokenProvider).getToken();
        EnedisApiClient enedisApi = new EnedisApiClient(tokenProvider, webClient);

        mockBackEnd.enqueue(TestResourceProvider.readMockResponseFromFile(TestResourceProvider.CONSUMPTION_LOAD_CURVE_1_DAY));
        String usagePointId = "24115050XXXXXX";
        LocalDate start = LocalDate.of(2024, 2, 26);
        LocalDate end = LocalDate.of(2024, 2, 27);

        // When & Then
        enedisApi.getProductionMeterReading(usagePointId, start, end, Granularity.PT30M)
                .as(StepVerifier::create)
                .assertNext(consumption -> {
                    // The API for some reason returns 47 readings instead of 48
                    assertEquals(47, consumption.intervalReadings().size());
                    assertEquals(usagePointId, consumption.usagePointId());
                    assertEquals(start, consumption.start());
                    assertEquals(end, consumption.end());
                    assertEquals(Granularity.PT30M.toString(),
                            consumption.intervalReadings().getFirst().intervalLength().get());
                })
                .expectComplete()
                .verify(Duration.ofSeconds(5));
    }

    @Test
    void getProductionMeterReading_7Days_PT1D_returnsConsumption() throws IOException {
        // Given
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        doReturn(Mono.just("token")).when(tokenProvider).getToken();
        EnedisApiClient enedisApi = new EnedisApiClient(tokenProvider, webClient);

        mockBackEnd.enqueue(TestResourceProvider.readMockResponseFromFile(TestResourceProvider.DAILY_CONSUMPTION_1_WEEK));
        String usagePointId = "24115050XXXXXX";
        LocalDate start = LocalDate.of(2024, 2, 1);
        LocalDate end = LocalDate.of(2024, 2, 8);
        // When & Then
        enedisApi.getProductionMeterReading(usagePointId, start, end, Granularity.P1D)
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
        // Given
        EnedisTokenProvider tokenProvider = mock(EnedisTokenProvider.class);
        doReturn(Mono.just("token")).when(tokenProvider).getToken();
        EnedisApiClient enedisApi = new EnedisApiClient(tokenProvider, webClient);

        // When & Then
        enedisApi.getConsumptionMeterReading("usagePointId", LocalDate.now(ZoneOffset.UTC),
                        LocalDate.now(ZoneOffset.UTC), Granularity.PT15M)
                .as(StepVerifier::create)
                .expectError(IllegalArgumentException.class)
                .verify(Duration.ofSeconds(5));
    }
}
