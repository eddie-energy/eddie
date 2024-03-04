package energy.eddie.regionconnector.dk.energinet.permission.request.states;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PastStateException;
import energy.eddie.api.agnostic.process.model.states.TerminatedPermissionRequestState;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.StateBuilderFactory;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EnerginetCustomerAcceptedStateTest {
    @Test
    void status_returnsAccepted() {
        // Given
        EnerginetCustomerAcceptedState state = new EnerginetCustomerAcceptedState(null, null);

        // When
        // Then
        assertEquals(PermissionProcessStatus.ACCEPTED, state.status());
    }

    @Test
    void terminate_changesToTerminated() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime end = start.plusDays(10);
        var creation = new PermissionRequestForCreation("cid", start, end, "token", Granularity.PT15M, "mpid", "dnid");
        StateBuilderFactory factory = new StateBuilderFactory();
        var permissionRequest = new EnerginetCustomerPermissionRequest("pid", creation, mock(EnerginetCustomerApi.class), factory);
        EnerginetCustomerAcceptedState state = new EnerginetCustomerAcceptedState(permissionRequest, factory);
        permissionRequest.changeState(state);

        // When
        state.terminate();

        // Then
        assertInstanceOf(TerminatedPermissionRequestState.class, permissionRequest.state());
    }

    @Test
    void revoke_changesToRevokeState() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime end = start.plusDays(10);
        var creation = new PermissionRequestForCreation("cid", start, end, "token", Granularity.PT15M, "mpid", "dnid");
        StateBuilderFactory factory = new StateBuilderFactory();
        var permissionRequest = new EnerginetCustomerPermissionRequest("pid", creation, mock(EnerginetCustomerApi.class), factory);
        EnerginetCustomerAcceptedState state = new EnerginetCustomerAcceptedState(permissionRequest, factory);
        permissionRequest.changeState(state);

        // When
        state.revoke();

        // Then
        assertEquals(PermissionProcessStatus.REVOKED, permissionRequest.state().status());
    }

    @Test
    void timeLimit_notImplemented() {
        // Given
        EnerginetCustomerAcceptedState state = new EnerginetCustomerAcceptedState(null, null);

        // When
        // Then
        assertThrows(UnsupportedOperationException.class, state::fulfill);
    }

    @Test
    void validate_throws() {
        // Given
        EnerginetCustomerAcceptedState state = new EnerginetCustomerAcceptedState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        EnerginetCustomerAcceptedState state = new EnerginetCustomerAcceptedState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        EnerginetCustomerAcceptedState state = new EnerginetCustomerAcceptedState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void accept_throws() {
        // Given
        EnerginetCustomerAcceptedState state = new EnerginetCustomerAcceptedState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        EnerginetCustomerAcceptedState state = new EnerginetCustomerAcceptedState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        EnerginetCustomerAcceptedState state = new EnerginetCustomerAcceptedState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::reject);
    }

    @Test
    void timeOut_throws() {
        // Given
        EnerginetCustomerAcceptedState state = new EnerginetCustomerAcceptedState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::timeOut);
    }
}