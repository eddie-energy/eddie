package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.TestResourceProvider;
import energy.eddie.regionconnector.fr.enedis.api.EnedisMeterReadingApi;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.api.UsagePointType;
import energy.eddie.regionconnector.fr.enedis.dto.readings.MeterReading;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrSimpleEvent;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequestBuilder;
import energy.eddie.regionconnector.fr.enedis.providers.MeterReadingType;
import energy.eddie.regionconnector.fr.enedis.tasks.UpdateGranularityTask;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.services.FulfillmentService;
import energy.eddie.regionconnector.shared.services.MeterReadingPermissionUpdateAndFulfillmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollingServiceTest {
    @Mock
    private Outbox outbox;
    @Mock
    private EnedisMeterReadingApi enedisApi;
    @Mock
    private UpdateGranularityTask updateGranularityTask;

    @Test
    void fetchHistoricalMeterReadingsThrowsForbidden_revokesPermissionRequest() {
        // Given
        doReturn(Mono.error(WebClientResponseException.create(HttpStatus.FORBIDDEN.value(), "", null, null, null)))
                .when(enedisApi).getConsumptionMeterReading(anyString(), any(), any(), any());
        MeterReadingPermissionUpdateAndFulfillmentService service = new MeterReadingPermissionUpdateAndFulfillmentService(
                new FulfillmentService(outbox, FrSimpleEvent::new),
                (pr, end) -> {}
        );
        EnergyDataStreams streams = new EnergyDataStreams();
        PollingService pollingService = new PollingService(enedisApi, service, outbox, updateGranularityTask, streams);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(20);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStart(start)
                .setEnd(end)
                .setGranularity(Granularity.P1D)
                .setUsagePointId("usagePointId")
                .create();

        // When
        pollingService.pollTimeSeriesData(request);

        // Then
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.REVOKED, event.status())));

        // Clean-Up
        streams.close();
    }

    @Test
    void fetchHistoricalMeterReadingsThrowsOther_doesNotRevokePermissionRequest() {
        // Given
        doReturn(Mono.error(WebClientResponseException.create(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                                              "",
                                                              null,
                                                              null,
                                                              null)))
                .when(enedisApi).getConsumptionMeterReading(anyString(), any(), any(), any());
        MeterReadingPermissionUpdateAndFulfillmentService service = new MeterReadingPermissionUpdateAndFulfillmentService(
                new FulfillmentService(outbox, FrSimpleEvent::new),
                (pr, end) -> {}
        );
        EnergyDataStreams streams = new EnergyDataStreams();
        PollingService pollingService = new PollingService(enedisApi, service, outbox, updateGranularityTask, streams);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(20);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStart(start)
                .setEnd(end)
                .setGranularity(Granularity.P1D)
                .setUsagePointId("usagePointId")
                .create();

        // When
        pollingService.pollTimeSeriesData(request);

        // Then
        verify(outbox, never()).commit(any());
        // Clean-Up
        streams.close();
    }

    @Test
    void fetchHistoricalMeterReadingsThrowsUnauthorized_retriesImmediately() {
        // Given
        when(enedisApi.getConsumptionMeterReading(anyString(), any(), any(), any()))
                .thenReturn(Mono.error(WebClientResponseException.create(HttpStatus.UNAUTHORIZED.value(),
                                                                         "",
                                                                         null,
                                                                         null,
                                                                         null)))
                .thenReturn(Mono.just(
                                    new MeterReading(
                                            "usagePointId",
                                            LocalDate.now(ZoneOffset.UTC),
                                            LocalDate.now(ZoneOffset.UTC),
                                            "BRUT",
                                            null,
                                            List.of()
                                    )
                            )
                );
        MeterReadingPermissionUpdateAndFulfillmentService service = new MeterReadingPermissionUpdateAndFulfillmentService(
                new FulfillmentService(outbox, FrSimpleEvent::new),
                (pr, end) -> {}
        );
        EnergyDataStreams streams = new EnergyDataStreams();
        PollingService pollingService = new PollingService(enedisApi, service, outbox, updateGranularityTask, streams);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStart(start)
                .setEnd(end)
                .setGranularity(Granularity.P1D)
                .setUsagePointId("usagePointId")
                .create();

        VirtualTimeScheduler.getOrSet(); // yes, this is necessary

        // When
        pollingService.pollTimeSeriesData(request);

        // Then
        StepVerifier.withVirtualTime(streams::getValidatedHistoricalData)
                    .thenAwait(Duration.ofMinutes(2))
                    .expectNextCount(1)
                    .then(streams::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
        verify(enedisApi, times(2)).getConsumptionMeterReading(anyString(), any(), any(), any());
    }

    @Test
    void fetchHistoricalMeterReadingsThrowsIllegalStateException_givenUnsupportedGranularity() {
        // Given
        MeterReadingPermissionUpdateAndFulfillmentService service = new MeterReadingPermissionUpdateAndFulfillmentService(
                new FulfillmentService(outbox, FrSimpleEvent::new),
                (pr, end) -> {}
        );
        EnergyDataStreams streams = new EnergyDataStreams();
        PollingService pollingService = new PollingService(enedisApi, service, outbox, updateGranularityTask, streams);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(20);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStart(start)
                .setEnd(end)
                .setGranularity(Granularity.P1Y)
                .setUsagePointId("usagePointId")
                .create();

        // When
        assertThrows(IllegalStateException.class,
                     () -> pollingService.pollTimeSeriesData(request));
        verifyNoInteractions(enedisApi);

        // Clean-Up
        streams.close();
    }

    @Test
    void fetchHistoricalMeterReadings_forConsumption() throws Exception {
        // Given
        MeterReadingPermissionUpdateAndFulfillmentService service = new MeterReadingPermissionUpdateAndFulfillmentService(
                new FulfillmentService(outbox, FrSimpleEvent::new),
                (pr, end) -> {}
        );
        EnergyDataStreams streams = new EnergyDataStreams();
        PollingService pollingService = new PollingService(enedisApi, service, outbox, updateGranularityTask, streams);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(20);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStart(start)
                .setEnd(end)
                .setGranularity(Granularity.P1D)
                .setUsagePointId("usagePointId")
                .create();

        when(enedisApi.getConsumptionMeterReading(anyString(), any(), any(), any()))
                .thenReturn(Mono.just(TestResourceProvider.readMeterReadingFromFile(TestResourceProvider.DAILY_CONSUMPTION_1_WEEK)));

        // When
        pollingService.pollTimeSeriesData(request);

        // Then
        StepVerifier.create(streams.getValidatedHistoricalData())
                    .assertNext(reading -> assertEquals(MeterReadingType.CONSUMPTION, reading.meterReadingType()))
                    .then(streams::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
    }

    @Test
    void fetchHistoricalMeterReadings_forConsumptionHalfHourly() throws Exception {
        // Given
        MeterReadingPermissionUpdateAndFulfillmentService service = new MeterReadingPermissionUpdateAndFulfillmentService(
                new FulfillmentService(outbox, FrSimpleEvent::new),
                (pr, end) -> {}
        );
        EnergyDataStreams streams = new EnergyDataStreams();
        PollingService pollingService = new PollingService(enedisApi, service, outbox, updateGranularityTask, streams);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(2);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        FrEnedisPermissionRequest request = new EnedisPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStart(start)
                .setEnd(end)
                .setGranularity(Granularity.PT30M)
                .setUsagePointId("usagePointId")
                .create();

        when(enedisApi.getConsumptionMeterReading(anyString(), any(), any(), any()))
                .thenReturn(Mono.just(TestResourceProvider.readMeterReadingFromFile(TestResourceProvider.CONSUMPTION_LOAD_CURVE_1_DAY)));

        // When
        pollingService.pollTimeSeriesData(request);

        // Then
        StepVerifier.create(streams.getValidatedHistoricalData())
                    .assertNext(reading -> assertEquals(MeterReadingType.CONSUMPTION, reading.meterReadingType()))
                    .then(streams::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
    }

    @Test
    void fetchHistoricalMeterReadings_forProduction() throws Exception {
        // Given
        MeterReadingPermissionUpdateAndFulfillmentService service = new MeterReadingPermissionUpdateAndFulfillmentService(
                new FulfillmentService(outbox, FrSimpleEvent::new),
                (pr, end) -> {}
        );
        EnergyDataStreams streams = new EnergyDataStreams();
        PollingService pollingService = new PollingService(enedisApi, service, outbox, updateGranularityTask, streams);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(20);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStart(start)
                .setEnd(end)
                .setGranularity(Granularity.P1D)
                .setStatus(PermissionProcessStatus.ACCEPTED)
                .setUsagePointId("usagePointId")
                .setUsagePointType(UsagePointType.PRODUCTION)
                .create();

        when(enedisApi.getProductionMeterReading(anyString(), any(), any(), any()))
                .thenReturn(Mono.just(TestResourceProvider.readMeterReadingFromFile(TestResourceProvider.DAILY_CONSUMPTION_1_WEEK)));

        // When
        pollingService.pollTimeSeriesData(request);

        // Then
        StepVerifier.create(streams.getValidatedHistoricalData())
                    .assertNext(reading -> assertEquals(MeterReadingType.PRODUCTION, reading.meterReadingType()))
                    .then(streams::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
    }

    @Test
    void fetchHistoricalMeterReadings_forConsumptionAndProduction() throws Exception {
        // Given
        MeterReadingPermissionUpdateAndFulfillmentService service = new MeterReadingPermissionUpdateAndFulfillmentService(
                new FulfillmentService(outbox, FrSimpleEvent::new),
                (pr, end) -> {}
        );
        EnergyDataStreams streams = new EnergyDataStreams();
        PollingService pollingService = new PollingService(enedisApi, service, outbox, updateGranularityTask, streams);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(20);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStart(start)
                .setEnd(end)
                .setGranularity(Granularity.P1D)
                .setUsagePointId("usagePointId")
                .setUsagePointType(UsagePointType.CONSUMPTION_AND_PRODUCTION)
                .create();

        when(enedisApi.getConsumptionMeterReading(anyString(), any(), any(), any()))
                .thenReturn(Mono.just(TestResourceProvider.readMeterReadingFromFile(TestResourceProvider.DAILY_CONSUMPTION_1_WEEK)));
        when(enedisApi.getProductionMeterReading(anyString(), any(), any(), any()))
                .thenReturn(Mono.just(TestResourceProvider.readMeterReadingFromFile(TestResourceProvider.DAILY_CONSUMPTION_1_WEEK)));

        // When
        pollingService.pollTimeSeriesData(request);

        // Then
        StepVerifier.create(streams.getValidatedHistoricalData())
                    .assertNext(reading -> assertEquals(MeterReadingType.CONSUMPTION, reading.meterReadingType()))
                    .assertNext(reading -> assertEquals(MeterReadingType.PRODUCTION, reading.meterReadingType()))
                    .then(streams::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
    }
}
