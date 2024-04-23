package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.dto.MeterReading;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrSimpleEvent;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.services.EventFulfillmentService;
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

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoricalDataServiceTest {
    @Mock
    private Outbox outbox;
    private final MeterReadingPermissionUpdateAndFulfillmentService service =
            new MeterReadingPermissionUpdateAndFulfillmentService(
                    new EventFulfillmentService(outbox, FrSimpleEvent::new),
                    (pr, end) -> {}
            );
    @Mock
    private EnedisApi enedisApi;

    @Test
    void fetchHistoricalMeterReadings_requestDataFor11Days_batchesFetchCallsInto2_andEmitsMeterReading() throws Exception {
        // Given
        Sinks.Many<IdentifiableMeterReading> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApi, service, sink, outbox);
        HistoricalDataService historicalDataService = new HistoricalDataService(pollingService);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(20);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequest("pid",
                                                                        "cid",
                                                                        "dnid",
                                                                        start,
                                                                        end,
                                                                        Granularity.PT30M,
                                                                        PermissionProcessStatus.ACCEPTED,
                                                                        "usagePointId",
                                                                        null,
                                                                        ZonedDateTime.now(ZoneOffset.UTC));

        when(enedisApi.getConsumptionMeterReading(anyString(), eq(start), eq(start.plusWeeks(1)), any()))
                .thenReturn(Mono.just(new MeterReading("usagePointId",
                                                       start,
                                                       start.plusWeeks(1),
                                                       "BRUT",
                                                       null,
                                                       List.of())));
        when(enedisApi.getConsumptionMeterReading(anyString(), eq(start.plusWeeks(1)), eq(end.plusDays(1)), any()))
                .thenReturn(Mono.just(new MeterReading("usagePointId",
                                                       start.plusWeeks(1),
                                                       end,
                                                       "BRUT",
                                                       null,
                                                       List.of())));
        // When
        historicalDataService.fetchHistoricalMeterReadings(request, "usagePointId");

        // Then
        StepVerifier.create(sink.asFlux())
                    .assertNext(cr -> {
                        assertEquals(start, cr.meterReading().start());
                        assertEquals(start.plusWeeks(1), cr.meterReading().end());
                    })
                    .assertNext(cr -> {
                        assertEquals(start.plusWeeks(1), cr.meterReading().start());
                        assertEquals(end, cr.meterReading().end());
                    })
                    .then(sink::tryEmitComplete)
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));

        // Clean-Up
        pollingService.close();
    }

    @Test
    void fetchHistoricalMeterReadings_requestDataFor20Days_SecondBatchFails_callsApiForOnlyFirst2Batch() throws Exception {
        // Given
        Sinks.Many<IdentifiableMeterReading> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApi, service, sink, outbox);
        HistoricalDataService historicalDataService = new HistoricalDataService(pollingService);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(30);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequest("pid",
                                                                        "cid",
                                                                        "dnid",
                                                                        start,
                                                                        end,
                                                                        Granularity.PT30M,
                                                                        PermissionProcessStatus.ACCEPTED,
                                                                        "usagePointId",
                                                                        null,
                                                                        ZonedDateTime.now(ZoneOffset.UTC));

        when(enedisApi.getConsumptionMeterReading(anyString(), eq(start), eq(start.plusWeeks(1)), any()))
                .thenReturn(Mono.just(new MeterReading("usagePointId",
                                                       start,
                                                       start.plusWeeks(1),
                                                       "BRUT",
                                                       null,
                                                       List.of())));
        when(enedisApi.getConsumptionMeterReading(anyString(), eq(start.plusWeeks(1)), eq(start.plusWeeks(2)), any()))
                .thenReturn(Mono.error(WebClientResponseException.create(HttpStatus.INTERNAL_SERVER_ERROR,
                                                                         "",
                                                                         null,
                                                                         null,
                                                                         null,
                                                                         null)));
        // When
        historicalDataService.fetchHistoricalMeterReadings(request, "usagePointId");

        // Then
        StepVerifier.create(sink.asFlux())
                    .assertNext(cr -> {
                        assertEquals(start, cr.meterReading().start());
                        assertEquals(start.plusWeeks(1), cr.meterReading().end());
                    })
                    .then(sink::tryEmitComplete)
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
        verify(enedisApi, times(1)).getConsumptionMeterReading(anyString(), eq(start), eq(start.plusWeeks(1)), any());
        verify(enedisApi, times(1)).getConsumptionMeterReading(anyString(),
                                                               eq(start.plusWeeks(1)),
                                                               eq(start.plusWeeks(2)),
                                                               any());
        verifyNoMoreInteractions(enedisApi);

        // Clean-Up
        pollingService.close();
    }

    @Test
    void fetchHistoricalMeterReadings_doesNothing_IfPermissionIsNotActive() throws Exception {
        // Given
        Sinks.Many<IdentifiableMeterReading> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApi, service, sink, outbox);
        HistoricalDataService historicalDataService = new HistoricalDataService(pollingService);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(20);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).plusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequest("pid",
                                                                        "cid",
                                                                        "dnid",
                                                                        start,
                                                                        end,
                                                                        Granularity.P1D,
                                                                        PermissionProcessStatus.CREATED,
                                                                        "usagePointId",
                                                                        null,
                                                                        ZonedDateTime.now(ZoneOffset.UTC));
        // When
        historicalDataService.fetchHistoricalMeterReadings(request, "usagePointId");

        // Then
        StepVerifier.create(sink.asFlux())
                    .then(sink::tryEmitComplete)
                    .verifyComplete();
        verifyNoInteractions(enedisApi);

        // Clean-Up
        pollingService.close();
    }
}
