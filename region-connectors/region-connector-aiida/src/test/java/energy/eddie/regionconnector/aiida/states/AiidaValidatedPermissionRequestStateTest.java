package energy.eddie.regionconnector.aiida.states;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.regionconnector.aiida.api.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiidaValidatedPermissionRequestStateTest {
    @Mock
    private AiidaRegionConnectorService mockService;

    @Test
    void status_returnsValidated() {
        // Given
        AiidaValidatedPermissionRequestState state = new AiidaValidatedPermissionRequestState(null, null);

        // When
        // Then
        assertEquals(PermissionProcessStatus.VALIDATED, state.status());
    }

    @Test
    void validate_throws() {
        // Given
        AiidaValidatedPermissionRequestState state = new AiidaValidatedPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void sendToPermissionAdministrator_callsService_andChangesToSentToPermissionAdministrator() {
        // Given
        var now = Instant.now();
        AiidaPermissionRequest permissionRequest = new AiidaPermissionRequest("foo", "bar",
                "loo", "too", now, now, mockService);
        var state = new AiidaValidatedPermissionRequestState(permissionRequest, mockService);
        permissionRequest.changeState(state);

        // When
        assertDoesNotThrow(permissionRequest::sendToPermissionAdministrator);

        // Then
        assertEquals(AiidaSentToPermissionAdministratorPermissionRequestState.class, permissionRequest.state().getClass());
        verify(mockService).sendToPermissionAdministrator(permissionRequest);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        AiidaValidatedPermissionRequestState state = new AiidaValidatedPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void accept_throws() {
        // Given
        AiidaValidatedPermissionRequestState state = new AiidaValidatedPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        AiidaValidatedPermissionRequestState state = new AiidaValidatedPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        AiidaValidatedPermissionRequestState state = new AiidaValidatedPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::reject);
    }

    @Test
    void terminate_throws() {
        // Given
        AiidaValidatedPermissionRequestState state = new AiidaValidatedPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::terminate);
    }

    @Test
    void revoke_throws() {
        // Given
        AiidaValidatedPermissionRequestState state = new AiidaValidatedPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::revoke);
    }

    @Test
    void timeLimit_throws() {
        // Given
        AiidaValidatedPermissionRequestState state = new AiidaValidatedPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::timeLimit);
    }

    @Test
    void timeOut_throws() {
        // Given
        AiidaValidatedPermissionRequestState state = new AiidaValidatedPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::timeOut);
    }
}