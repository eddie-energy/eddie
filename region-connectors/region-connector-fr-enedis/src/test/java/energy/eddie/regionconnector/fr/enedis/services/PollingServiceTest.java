package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.dto.MeterReading;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.StateBuilderFactory;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PollingServiceTest {
    @Test
    void fetchHistoricalMeterReadingsThrowsForbidden_revokesPermissionRequest() throws Exception {
        // Given
        EnedisApi enedisApi = mock(EnedisApi.class);
        doReturn(Mono.error(WebClientResponseException.create(HttpStatus.FORBIDDEN.value(), "", null, null, null)))
                .when(enedisApi).getConsumptionMeterReading(anyString(), any(), any(), any());

        Sinks.Many<IdentifiableMeterReading> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApi, sink);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(20);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        StateBuilderFactory factory = new StateBuilderFactory();
        FrEnedisPermissionRequest request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end, Granularity.P1D, factory);
        request.changeState(new FrEnedisAcceptedState(request, factory));
        request.setUsagePointId("usagePointId");

        // When
        pollingService.fetchMeterReadings(request, start, end);

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
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(20);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        StateBuilderFactory factory = new StateBuilderFactory();
        FrEnedisPermissionRequest request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end, Granularity.P1D, factory);
        request.changeState(new FrEnedisAcceptedState(request, factory));
        request.setUsagePointId("usagePointId");

        // When
        pollingService.fetchMeterReadings(request, start, end);

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
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        StateBuilderFactory factory = new StateBuilderFactory();
        FrEnedisPermissionRequest request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end, Granularity.P1D, factory);
        request.changeState(new FrEnedisAcceptedState(request, factory));
        request.setUsagePointId("usagePointId");

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
    void fetchHistoricalMeterReadingsThrowsForbidden_doesNotRevokeForNotAcceptedPermissionRequest() throws Exception {
        // Given
        EnedisApi enedisApi = mock(EnedisApi.class);
        doReturn(Mono.error(WebClientResponseException.create(HttpStatus.FORBIDDEN.value(), "", null, null, null)))
                .when(enedisApi).getConsumptionMeterReading(anyString(), any(), any(), any());

        Sinks.Many<IdentifiableMeterReading> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApi, sink);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(20);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        StateBuilderFactory factory = new StateBuilderFactory();
        FrEnedisPermissionRequest request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end, Granularity.P1D, factory);
        request.setUsagePointId("usagePointId");

        // When
        pollingService.fetchMeterReadings(request, start, end);

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
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(20);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        StateBuilderFactory factory = new StateBuilderFactory();
        FrEnedisPermissionRequest request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end, Granularity.PT15M, factory);
        request.setUsagePointId("usagePointId");

        // When
        assertThrows(IllegalStateException.class, () -> pollingService.fetchMeterReadings(request, start, end));
        verifyNoInteractions(enedisApi);

        // Clean-Up
        pollingService.close();
    }
}
