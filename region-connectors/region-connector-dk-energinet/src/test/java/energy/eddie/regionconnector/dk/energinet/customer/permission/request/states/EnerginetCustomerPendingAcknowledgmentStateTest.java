package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import org.junit.jupiter.api.Test;

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
        var start = ZonedDateTime.now(EnerginetRegionConnector.DK_ZONE_ID).minusDays(10);
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        String meteringPoint = "meteringPoint";
        Granularity granularity = Granularity.PT1H;
        String connectionId = "cid";
        String dataNeedId = "dataNeedId";
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);
        var forCreation = new PermissionRequestForCreation(connectionId, start, start.plusDays(5), refreshToken, granularity, meteringPoint, dataNeedId);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, forCreation, config);
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