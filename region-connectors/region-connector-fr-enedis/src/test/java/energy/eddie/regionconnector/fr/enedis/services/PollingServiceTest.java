package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.dto.MeterReading;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.states.FrEnedisAcceptedState;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import org.junit.jupiter.api.Test;
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

class PollingServiceTest {

    @Test
    void fetchHistoricalMeterReadings_requestDataFor11Days_batchesFetchCallsInto2_andEmitsMeterReading() throws Exception {
        // Given
        EnedisApi enedisApi = mock(EnedisApi.class);
        Sinks.Many<IdentifiableMeterReading> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApi, sink);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(20);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequest("pid", "cid", "dnid", start.atStartOfDay(ZoneOffset.UTC), end.atStartOfDay(ZoneOffset.UTC), Granularity.PT30M);
        request.setUsagePointId("usagePointId");

        when(enedisApi.getConsumptionMeterReading(anyString(), eq(start), eq(start.plusWeeks(1)), any()))
                .thenReturn(Mono.just(new MeterReading("usagePointId", start, start.plusWeeks(1), "BRUT", null, List.of())));
        when(enedisApi.getConsumptionMeterReading(anyString(), eq(start.plusWeeks(1)), eq(end.plusDays(1)), any()))
                .thenReturn(Mono.just(new MeterReading("usagePointId", start.plusWeeks(1), end, "BRUT", null, List.of())));
        // When
        pollingService.fetchHistoricalMeterReadings(request);

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
        EnedisApi enedisApi = mock(EnedisApi.class);
        Sinks.Many<IdentifiableMeterReading> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApi, sink);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(30);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequest("pid", "cid", "dnid", start.atStartOfDay(ZoneOffset.UTC), end.atStartOfDay(ZoneOffset.UTC), Granularity.PT30M);
        request.setUsagePointId("usagePointId");

        when(enedisApi.getConsumptionMeterReading(anyString(), eq(start), eq(start.plusWeeks(1)), any()))
                .thenReturn(Mono.just(new MeterReading("usagePointId", start, start.plusWeeks(1), "BRUT", null, List.of())));
        when(enedisApi.getConsumptionMeterReading(anyString(), eq(start.plusWeeks(1)), eq(start.plusWeeks(2)), any()))
                .thenReturn(Mono.error(WebClientResponseException.create(HttpStatus.INTERNAL_SERVER_ERROR, "", null, null, null, null)));
        // When
        pollingService.fetchHistoricalMeterReadings(request);

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
        verify(enedisApi, times(1)).getConsumptionMeterReading(anyString(), eq(start.plusWeeks(1)), eq(start.plusWeeks(2)), any());
        verifyNoMoreInteractions(enedisApi);

        // Clean-Up
        pollingService.close();
    }

    @Test
    void fetchHistoricalMeterReadings_doesNothing_IfPermissionIsNotActive() throws Exception {
        // Given
        EnedisApi enedisApi = mock(EnedisApi.class);
        Sinks.Many<IdentifiableMeterReading> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApi, sink);
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).plusDays(20);
        ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC).plusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end, Granularity.P1D);
        request.setUsagePointId("usagePointId");
        // When
        pollingService.fetchHistoricalMeterReadings(request);

        // Then
        StepVerifier.create(sink.asFlux())
                .then(sink::tryEmitComplete)
                .verifyComplete();
        verifyNoInteractions(enedisApi);

        // Clean-Up
        pollingService.close();
    }

    @Test
    void fetchHistoricalMeterReadingsThrowsForbidden_revokesPermissionRequest() throws Exception {
        // Given
        EnedisApi enedisApi = mock(EnedisApi.class);
        doReturn(Mono.error(WebClientResponseException.create(HttpStatus.FORBIDDEN.value(), "", null, null, null)))
                .when(enedisApi).getConsumptionMeterReading(anyString(), any(), any(), any());

        Sinks.Many<IdentifiableMeterReading> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApi, sink);
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(20);
        ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end, Granularity.P1D);
        request.changeState(new FrEnedisAcceptedState(request));
        request.setUsagePointId("usagePointId");

        // When
        pollingService.fetchHistoricalMeterReadings(request);

        // Then
        assertEquals(PermissionProcessStatus.REVOKED, request.state().status());

        // Clean-Up
        pollingService.close();
    }

    @Test
    void fetchHistoricalMeterReadingsThrowsOther_doesNotRevokePermissionRequest() throws Exception {
        // Given
        EnedisApi enedisApi = mock(EnedisApi.class);
        doReturn(Mono.error(WebClientResponseException.create(HttpStatus.INTERNAL_SERVER_ERROR.value(), "", null, null, null)))
                .when(enedisApi).getConsumptionMeterReading(anyString(), any(), any(), any());

        Sinks.Many<IdentifiableMeterReading> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApi, sink);
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(20);
        ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end, Granularity.P1D);
        request.changeState(new FrEnedisAcceptedState(request));
        request.setUsagePointId("usagePointId");

        // When
        pollingService.fetchHistoricalMeterReadings(request);

        // Then
        assertEquals(PermissionProcessStatus.ACCEPTED, request.state().status());

        // Clean-Up
        pollingService.close();
    }

    @Test
    void fetchHistoricalMeterReadingsThrowsUnauthorized_RetriesImmediately() throws Exception {
        // Given
        EnedisApi enedisApi = mock(EnedisApi.class);
        when(enedisApi.getConsumptionMeterReading(anyString(), any(), any(), any()))
                .thenReturn(Mono.error(WebClientResponseException.create(HttpStatus.UNAUTHORIZED.value(), "", null, null, null)))
                .thenReturn(Mono.just(mock(MeterReading.class)));

        Sinks.Many<IdentifiableMeterReading> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApi, sink);
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10);
        ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end, Granularity.P1D);
        request.changeState(new FrEnedisAcceptedState(request));
        request.setUsagePointId("usagePointId");

        VirtualTimeScheduler.getOrSet(); // yes, this is necessary

        // When
        pollingService.fetchHistoricalMeterReadings(request);

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
    void fetchHistoricalMeterReadingsThrowsForbidden_doesNotRevokeForNotAcceptedPermissionRequest() throws Exception {
        // Given
        EnedisApi enedisApi = mock(EnedisApi.class);
        doReturn(Mono.error(WebClientResponseException.create(HttpStatus.FORBIDDEN.value(), "", null, null, null)))
                .when(enedisApi).getConsumptionMeterReading(anyString(), any(), any(), any());

        Sinks.Many<IdentifiableMeterReading> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApi, sink);
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(20);
        ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end, Granularity.P1D);
        request.setUsagePointId("usagePointId");

        // When
        pollingService.fetchHistoricalMeterReadings(request);

        // Then
        assertEquals(PermissionProcessStatus.CREATED, request.state().status());

        // Clean-Up
        pollingService.close();
    }


    @Test
    void fetchHistoricalMeterReadingsThrowsIllegalStateException_givenUnsupportedGranularity() throws Exception {
        // Given
        EnedisApi enedisApi = mock(EnedisApi.class);
        Sinks.Many<IdentifiableMeterReading> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApi, sink);
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(20);
        ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end, Granularity.PT15M);
        request.setUsagePointId("usagePointId");

        // When
        assertThrows(IllegalStateException.class, () -> pollingService.fetchHistoricalMeterReadings(request));
        verifyNoInteractions(enedisApi);

        // Clean-Up
        pollingService.close();
    }
}