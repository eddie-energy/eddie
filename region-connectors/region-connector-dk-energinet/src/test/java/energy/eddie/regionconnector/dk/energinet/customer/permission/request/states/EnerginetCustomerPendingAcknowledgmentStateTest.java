package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.enums.PeriodResolutionEnum;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class EnerginetCustomerPendingAcknowledgmentStateTest {
    @Test
    void status_returnsPendingAcknowledgement() {
        // Given
        EnerginetCustomerPendingAcknowledgmentState state = new EnerginetCustomerPendingAcknowledgmentState(null);

        // When
        // Then
        assertEquals(PermissionProcessStatus.PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT, state.status());
    }
    
    @Test
    void receivedPermissionAdminAnswer_transitionsState() {
        // Given
        var start = ZonedDateTime.now(ZoneId.of("Europe/Copenhagen")).minusDays(10);
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        String meteringPoint = "meteringPoint";
        PeriodResolutionEnum resolution = PeriodResolutionEnum.PT1H;
        String connectionId = "cid";
        String dataNeedId = "dataNeedId";
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, connectionId, start,
                start.plusDays(5), refreshToken, meteringPoint, dataNeedId, resolution, config);
        EnerginetCustomerPendingAcknowledgmentState state = new EnerginetCustomerPendingAcknowledgmentState(permissionRequest);

        // When
        state.receivedPermissionAdministratorResponse();

        // Then
        assertEquals(EnerginetCustomerSentToPermissionAdministratorState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_throws() {
        // Given
        EnerginetCustomerPendingAcknowledgmentState state = new EnerginetCustomerPendingAcknowledgmentState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        EnerginetCustomerPendingAcknowledgmentState state = new EnerginetCustomerPendingAcknowledgmentState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void accept_throws() {
        // Given
        EnerginetCustomerPendingAcknowledgmentState state = new EnerginetCustomerPendingAcknowledgmentState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        EnerginetCustomerPendingAcknowledgmentState state = new EnerginetCustomerPendingAcknowledgmentState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        EnerginetCustomerPendingAcknowledgmentState state = new EnerginetCustomerPendingAcknowledgmentState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::reject);
    }

    @Test
    void terminate_throws() {
        // Given
        EnerginetCustomerPendingAcknowledgmentState state = new EnerginetCustomerPendingAcknowledgmentState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::terminate);
    }

    @Test
    void revoke_throws() {
        // Given
        EnerginetCustomerPendingAcknowledgmentState state = new EnerginetCustomerPendingAcknowledgmentState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::revoke);
    }

    @Test
    void timeLimit_throws() {
        // Given
        EnerginetCustomerPendingAcknowledgmentState state = new EnerginetCustomerPendingAcknowledgmentState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::timeLimit);
    }

    @Test
    void timeOut_throws() {
        // Given
        EnerginetCustomerPendingAcknowledgmentState state = new EnerginetCustomerPendingAcknowledgmentState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::timeOut);
    }
}
