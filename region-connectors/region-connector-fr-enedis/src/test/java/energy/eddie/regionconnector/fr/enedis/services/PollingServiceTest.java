package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.TestResourceProvider;
import energy.eddie.regionconnector.fr.enedis.api.EnedisMeterReadingApi;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.api.UsagePointType;
import energy.eddie.regionconnector.fr.enedis.dto.MeterReading;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrSimpleEvent;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import energy.eddie.regionconnector.fr.enedis.providers.MeterReadingType;
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
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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

    @Test
    void fetchHistoricalMeterReadingsThrowsForbidden_revokesPermissionRequest() throws Exception {
        // Given
        doReturn(Mono.error(WebClientResponseException.create(HttpStatus.FORBIDDEN.value(), "", null, null, null)))
                .when(enedisApi).getConsumptionMeterReading(anyString(), any(), any(), any());
        MeterReadingPermissionUpdateAndFulfillmentService service = new MeterReadingPermissionUpdateAndFulfillmentService(
                new FulfillmentService(outbox, FrSimpleEvent::new),
                (pr, end) -> {}
        );
        Sinks.Many<IdentifiableMeterReading> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApi, service, sink, outbox);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(20);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequest("pid",
                                                                        "cid",
                                                                        "dnid",
                                                                        start,
                                                                        end,
                                                                        Granularity.P1D,
                                                                        PermissionProcessStatus.ACCEPTED,
                                                                        "usagePointId",
                                                                        null,
                                                                        ZonedDateTime.now(ZoneOffset.UTC),
                                                                        UsagePointType.CONSUMPTION);

        // When
        pollingService.fetchMeterReadings(request, start, end);

        // Then
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.REVOKED, event.status())));

        // Clean-Up
        pollingService.close();
    }

    @Test
    void fetchHistoricalMeterReadingsThrowsOther_doesNotRevokePermissionRequest() throws Exception {
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
        Sinks.Many<IdentifiableMeterReading> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApi, service, sink, outbox);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(20);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequest("pid",
                                                                        "cid",
                                                                        "dnid",
                                                                        start,
                                                                        end,
                                                                        Granularity.P1D,
                                                                        PermissionProcessStatus.ACCEPTED,
                                                                        "usagePointId",
                                                                        null,
                                                                        ZonedDateTime.now(ZoneOffset.UTC),
                                                                        UsagePointType.CONSUMPTION);

        // When
        pollingService.fetchMeterReadings(request, start, end);

        // Then
        verify(outbox, never()).commit(any());
        // Clean-Up
        pollingService.close();
    }

    @Test
    void fetchHistoricalMeterReadingsThrowsUnauthorized_retriesImmediately() throws Exception {
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
        Sinks.Many<IdentifiableMeterReading> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApi, service, sink, outbox);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequest("pid",
                                                                        "cid",
                                                                        "dnid",
                                                                        start,
                                                                        end,
                                                                        Granularity.P1D,
                                                                        PermissionProcessStatus.ACCEPTED,
                                                                        "usagePointId",
                                                                        null,
                                                                        ZonedDateTime.now(ZoneOffset.UTC),
                                                                        UsagePointType.CONSUMPTION);

        VirtualTimeScheduler.getOrSet(); // yes, this is necessary

        // When
        pollingService.fetchMeterReadings(request, start, end);

        // Then
        StepVerifier.withVirtualTime(sink::asFlux)
                    .thenAwait(Duration.ofMinutes(2))
                    .expectNextCount(1)
                    .then(sink::tryEmitComplete)
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
        verify(enedisApi, times(2)).getConsumptionMeterReading(anyString(), any(), any(), any());

        // Clean-Up
        pollingService.close();
    }

    @Test
    void fetchHistoricalMeterReadingsThrowsIllegalStateException_givenUnsupportedGranularity() throws Exception {
        // Given
        MeterReadingPermissionUpdateAndFulfillmentService service = new MeterReadingPermissionUpdateAndFulfillmentService(
                new FulfillmentService(outbox, FrSimpleEvent::new),
                (pr, end) -> {}
        );
        Sinks.Many<IdentifiableMeterReading> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApi, service, sink, outbox);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(20);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequest("pid",
                                                                        "cid",
                                                                        "dnid",
                                                                        start,
                                                                        end,
                                                                        Granularity.P1Y,
                                                                        PermissionProcessStatus.ACCEPTED,
                                                                        "usagePointId",
                                                                        null,
                                                                        ZonedDateTime.now(ZoneOffset.UTC),
                                                                        UsagePointType.CONSUMPTION);

        // When
        assertThrows(IllegalStateException.class,
                     () -> pollingService.fetchMeterReadings(request, start, end));
        verifyNoInteractions(enedisApi);

        // Clean-Up
        pollingService.close();
    }

    @Test
    void fetchHistoricalMeterReadings_forConsumption() throws Exception {
        // Given
        MeterReadingPermissionUpdateAndFulfillmentService service = new MeterReadingPermissionUpdateAndFulfillmentService(
                new FulfillmentService(outbox, FrSimpleEvent::new),
                (pr, end) -> {}
        );
        Sinks.Many<IdentifiableMeterReading> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApi, service, sink, outbox);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(20);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequest(
                "pid",
                "cid",
                "dnid",
                start,
                end,
                Granularity.P1D,
                PermissionProcessStatus.ACCEPTED,
                "usagePointId",
                null,
                ZonedDateTime.now(ZoneOffset.UTC),
                UsagePointType.CONSUMPTION
        );

        when(enedisApi.getConsumptionMeterReading(anyString(), any(), any(), any()))
                .thenReturn(Mono.just(TestResourceProvider.readMeterReadingFromFile(TestResourceProvider.DAILY_CONSUMPTION_1_WEEK)));

        // When
        pollingService.fetchMeterReadings(request, start, end);

        // Then
        StepVerifier.create(sink.asFlux())
                    .assertNext(reading -> assertEquals(MeterReadingType.CONSUMPTION, reading.meterReadingType()))
                    .then(sink::tryEmitComplete)
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));

        pollingService.close();
    }

    @Test
    void fetchHistoricalMeterReadings_forProduction() throws Exception {
        // Given
        MeterReadingPermissionUpdateAndFulfillmentService service = new MeterReadingPermissionUpdateAndFulfillmentService(
                new FulfillmentService(outbox, FrSimpleEvent::new),
                (pr, end) -> {}
        );
        Sinks.Many<IdentifiableMeterReading> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApi, service, sink, outbox);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(20);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequest(
                "pid",
                "cid",
                "dnid",
                start,
                end,
                Granularity.P1D,
                PermissionProcessStatus.ACCEPTED,
                "usagePointId",
                null,
                ZonedDateTime.now(ZoneOffset.UTC),
                UsagePointType.PRODUCTION
        );

        when(enedisApi.getProductionMeterReading(anyString(), any(), any(), any()))
                .thenReturn(Mono.just(TestResourceProvider.readMeterReadingFromFile(TestResourceProvider.DAILY_CONSUMPTION_1_WEEK)));

        // When
        pollingService.fetchMeterReadings(request, start, end);

        // Then
        StepVerifier.create(sink.asFlux())
                    .assertNext(reading -> assertEquals(MeterReadingType.PRODUCTION, reading.meterReadingType()))
                    .then(sink::tryEmitComplete)
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));

        pollingService.close();
    }

    @Test
    void fetchHistoricalMeterReadings_forConsumptionAndProduction() throws Exception {
        // Given
        MeterReadingPermissionUpdateAndFulfillmentService service = new MeterReadingPermissionUpdateAndFulfillmentService(
                new FulfillmentService(outbox, FrSimpleEvent::new),
                (pr, end) -> {}
        );
        Sinks.Many<IdentifiableMeterReading> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApi, service, sink, outbox);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(20);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequest(
                "pid",
                "cid",
                "dnid",
                start,
                end,
                Granularity.P1D,
                PermissionProcessStatus.ACCEPTED,
                "usagePointId",
                null,
                ZonedDateTime.now(ZoneOffset.UTC),
                UsagePointType.CONSUMPTION_AND_PRODUCTION
        );

        when(enedisApi.getConsumptionMeterReading(anyString(), any(), any(), any()))
                .thenReturn(Mono.just(TestResourceProvider.readMeterReadingFromFile(TestResourceProvider.DAILY_CONSUMPTION_1_WEEK)));
        when(enedisApi.getProductionMeterReading(anyString(), any(), any(), any()))
                .thenReturn(Mono.just(TestResourceProvider.readMeterReadingFromFile(TestResourceProvider.DAILY_CONSUMPTION_1_WEEK)));

        // When
        pollingService.fetchMeterReadings(request, start, end);

        // Then
        StepVerifier.create(sink.asFlux())
                    .assertNext(reading -> assertEquals(MeterReadingType.CONSUMPTION, reading.meterReadingType()))
                    .assertNext(reading -> assertEquals(MeterReadingType.PRODUCTION, reading.meterReadingType()))
                    .then(sink::tryEmitComplete)
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));

        pollingService.close();
    }
}
