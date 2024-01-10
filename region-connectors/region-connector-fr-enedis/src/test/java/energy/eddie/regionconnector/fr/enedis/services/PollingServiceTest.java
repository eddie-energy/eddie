package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.invoker.ApiException;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.states.FrEnedisAcceptedState;
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
    void testRequestData_emitsConsumptionRecord() throws ApiException {
        // Given
        EnedisApiService enedisApiService = mock(EnedisApiService.class);
        when(enedisApiService.getConsumptionLoadCurve(anyString(), any(), any()))
                .thenReturn(new ConsumptionRecord())
                .thenReturn(new ConsumptionRecord());
        Sinks.Many<ConsumptionRecord> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApiService, sink);
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(20);
        ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10);
        TimeframedPermissionRequest request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end);

        // When
        pollingService.requestData(request, "usagePointId");

        // Then
        StepVerifier.create(sink.asFlux())
                .assertNext(cr -> assertEquals("cid", cr.getConnectionId()))
                .assertNext(cr -> assertEquals("cid", cr.getConnectionId()))
                .then(sink::tryEmitComplete)
                .verifyComplete();
    }

    @Test
    void testRequestDataThrowsForbidden_revokesPermissionRequest() throws ApiException {
        // Given
        EnedisApiService enedisApiService = mock(EnedisApiService.class);
        when(enedisApiService.getConsumptionLoadCurve(anyString(), any(), any()))
                .thenThrow(new ApiException(HttpStatus.FORBIDDEN.value(), ""));
        Sinks.Many<ConsumptionRecord> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApiService, sink);
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(20);
        ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10);
        TimeframedPermissionRequest request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end);
        request.changeState(new FrEnedisAcceptedState(request));

        // When
        pollingService.requestData(request, "usagePointId");

        // Then
        assertEquals(PermissionProcessStatus.REVOKED, request.state().status());
    }

    @Test
    void testRequestDataThrowsOther_doesNotRevokePermissionRequest() throws ApiException {
        // Given
        EnedisApiService enedisApiService = mock(EnedisApiService.class);
        when(enedisApiService.getConsumptionLoadCurve(anyString(), any(), any()))
                .thenThrow(new ApiException(HttpStatus.INTERNAL_SERVER_ERROR.value(), ""));
        Sinks.Many<ConsumptionRecord> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApiService, sink);
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(20);
        ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10);
        TimeframedPermissionRequest request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end);
        request.changeState(new FrEnedisAcceptedState(request));

        // When
        pollingService.requestData(request, "usagePointId");

        // Then
        assertEquals(PermissionProcessStatus.ACCEPTED, request.state().status());
    }

    @Test
    void testRequestDataThrowsForbidden_doesNotRevokeForNotAcceptedPermissionRequest() throws ApiException {
        // Given
        EnedisApiService enedisApiService = mock(EnedisApiService.class);
        when(enedisApiService.getConsumptionLoadCurve(anyString(), any(), any()))
                .thenThrow(new ApiException(HttpStatus.FORBIDDEN.value(), ""));
        Sinks.Many<ConsumptionRecord> sink = Sinks.many().multicast().onBackpressureBuffer();
        PollingService pollingService = new PollingService(enedisApiService, sink);
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(20);
        ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10);
        TimeframedPermissionRequest request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end);

        // When
        pollingService.requestData(request, "usagePointId");

        // Then
        assertEquals(PermissionProcessStatus.CREATED, request.state().status());
    }


}