package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.states.RevokedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.TerminatedPermissionRequestState;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.StateBuilderFactory;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class AcceptedStateTest {
    StateBuilderFactory factory = new StateBuilderFactory(mock(AuthorizationApi.class));

    @Test
    void terminate_terminatesPermissionRequest() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime end = start.plusDays(10);
        var permissionRequest = new DatadisPermissionRequest(
                "pid",
                new PermissionRequestForCreation("cid", "dnid", "nif", "mpid", start, end, Granularity.PT15M),
                factory
        );
        AcceptedState state = new AcceptedState(permissionRequest, factory);

        // When
        state.terminate();

        // Then
        assertInstanceOf(TerminatedPermissionRequestState.class, permissionRequest.state());
    }

    @Test
    void revoke_revokesPermissionRequest() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime end = start.plusDays(10);
        var permissionRequest = new DatadisPermissionRequest(
                "pid",
                new PermissionRequestForCreation("cid", "dnid", "nif", "mpid", start, end, Granularity.PT15M),
                factory
        );
        AcceptedState state = new AcceptedState(permissionRequest, factory);

        // When
        state.revoke();

        // Then
        assertInstanceOf(RevokedPermissionRequestState.class, permissionRequest.state());
    }

    @Test
    void fulfill_fulfillsPermissionRequest() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime end = start.plusDays(10);
        var permissionRequest = new DatadisPermissionRequest(
                "pid",
                new PermissionRequestForCreation("cid", "dnid", "nif", "mpid", start, end, Granularity.PT15M),
                factory
        );
        AcceptedState state = new AcceptedState(permissionRequest, factory);

        // When
        state.fulfill();

        // Then
        assertInstanceOf(FulfilledState.class, permissionRequest.state());
    }
}
