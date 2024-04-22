package energy.eddie.regionconnector.dk.energinet.permission.request.states;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PastStateException;
import energy.eddie.api.agnostic.process.model.states.TerminatedPermissionRequestState;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.DkEnerginetSpringConfig;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.StateBuilderFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EnerginetCustomerAcceptedStateTest {
    private final LocalDate start = LocalDate.now(ZoneOffset.UTC);
    private final LocalDate end = start.plusDays(10);
    private final PermissionRequestForCreation creation = new PermissionRequestForCreation("cid",
                                                                                           "token",
                                                                                           "mpid",
                                                                                           "dnid");
    private final StateBuilderFactory factory = new StateBuilderFactory();
    private final ObjectMapper mapper = new DkEnerginetSpringConfig().objectMapper();
    private final EnerginetCustomerPermissionRequest permissionRequest = new EnerginetCustomerPermissionRequest("pid",
                                                                                                                creation,
                                                                                                                mock(EnerginetCustomerApi.class),
                                                                                                                start,
                                                                                                                end,
                                                                                                                Granularity.PT15M,
                                                                                                                factory,
                                                                                                                mapper);

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
        EnerginetCustomerAcceptedState state = new EnerginetCustomerAcceptedState(permissionRequest, factory);
        permissionRequest.changeState(state);

        // When
        state.revoke();

        // Then
        assertEquals(PermissionProcessStatus.REVOKED, permissionRequest.state().status());
    }

    @Test
    void fulfill_changesToFulfilledState() {
        // Given
        EnerginetCustomerAcceptedState state = new EnerginetCustomerAcceptedState(permissionRequest, factory);
        permissionRequest.changeState(state);


        // When
        state.fulfill();

        // Then
        assertEquals(PermissionProcessStatus.FULFILLED, permissionRequest.state().status());
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
