package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.invoker.ApiException;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveMeterReading;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.states.FrEnedisAcceptedState;
import energy.eddie.regionconnector.fr.enedis.providers.agnostic.IdentifiableMeterReading;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PollingServiceTest {

    @Test
    void testRequestData_emitsConsumptionRecord() throws Exception {
        // Given
        EnedisApiService enedisApiService = mock(EnedisApiService.class);
        when(enedisApiService.getConsumptionLoadCurve(anyString(), any(), any()))
                .thenReturn(new ConsumptionLoadCurveMeterReading())
                .thenReturn(new ConsumptionLoadCurveMeterReading());
        Sinks.Many<IdentifiableMeterReading> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApiService, sink);
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(20);
        ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end, Granularity.P1D);

        // When
        pollingService.requestData(request, "usagePointId");

        // Then
        StepVerifier.create(sink.asFlux())
                .assertNext(cr -> assertEquals("cid", cr.connectionId()))
                .assertNext(cr -> assertEquals("cid", cr.connectionId()))
                .then(sink::tryEmitComplete)
                .verifyComplete();

        // Clean-Up
        pollingService.close();
    }

    @Test
    void testRequestDataThrowsForbidden_revokesPermissionRequest() throws Exception {
        // Given
        EnedisApiService enedisApiService = mock(EnedisApiService.class);
        when(enedisApiService.getConsumptionLoadCurve(anyString(), any(), any()))
                .thenThrow(new ApiException(HttpStatus.FORBIDDEN.value(), ""));
        Sinks.Many<IdentifiableMeterReading> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApiService, sink);
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(20);
        ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end, Granularity.P1D);
        request.changeState(new FrEnedisAcceptedState(request));

        // When
        pollingService.requestData(request, "usagePointId");

        // Then
        assertEquals(PermissionProcessStatus.REVOKED, request.state().status());

        // Clean-Up
        pollingService.close();
    }

    @Test
    void testRequestDataThrowsOther_doesNotRevokePermissionRequest() throws Exception {
        // Given
        EnedisApiService enedisApiService = mock(EnedisApiService.class);
        when(enedisApiService.getConsumptionLoadCurve(anyString(), any(), any()))
                .thenThrow(new ApiException(HttpStatus.INTERNAL_SERVER_ERROR.value(), ""));
        Sinks.Many<IdentifiableMeterReading> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApiService, sink);
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(20);
        ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end, Granularity.P1D);
        request.changeState(new FrEnedisAcceptedState(request));

        // When
        pollingService.requestData(request, "usagePointId");

        // Then
        assertEquals(PermissionProcessStatus.ACCEPTED, request.state().status());

        // Clean-Up
        pollingService.close();
    }

    @Test
    void testRequestDataThrowsForbidden_doesNotRevokeForNotAcceptedPermissionRequest() throws Exception {
        // Given
        EnedisApiService enedisApiService = mock(EnedisApiService.class);
        when(enedisApiService.getConsumptionLoadCurve(anyString(), any(), any()))
                .thenThrow(new ApiException(HttpStatus.FORBIDDEN.value(), ""));
        Sinks.Many<IdentifiableMeterReading> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApiService, sink);
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(20);
        ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10);
        FrEnedisPermissionRequest request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end, Granularity.P1D);

        // When
        pollingService.requestData(request, "usagePointId");

        // Then
        assertEquals(PermissionProcessStatus.CREATED, request.state().status());

        // Clean-Up
        pollingService.close();
    }
}