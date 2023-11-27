package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.enums.PeriodResolutionEnum;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EnerginetCustomerSentToPermissionAdministratorStateTest {
    @Test
    void status_returnsSentToPermissionAdministrator() {
        // Given
        EnerginetCustomerSentToPermissionAdministratorState state = new EnerginetCustomerSentToPermissionAdministratorState(null);

        // When
        // Then
        assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, state.status());
    }

    @Test
    void accept_changesToAcceptedState() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(EnerginetRegionConnector.DK_ZONE_ID).minusDays(30);
        ZonedDateTime end = start.plusDays(10);
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        String meteringPoint = "meteringPoint";
        PeriodResolutionEnum resolution = PeriodResolutionEnum.PT1H;
        String connectionId = "cid";
        String dataNeedId = "dataNeedId";
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, connectionId, start, end,
                refreshToken, meteringPoint, dataNeedId, resolution, config);
        var state = new EnerginetCustomerSentToPermissionAdministratorState(permissionRequest);
        permissionRequest.changeState(state);

        // When
        assertDoesNotThrow(permissionRequest::accept);

        // Then
        assertEquals(EnerginetCustomerAcceptedState.class, permissionRequest.state().getClass());
    }

    @Test
    void invalid_changesToInvalidState() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(EnerginetRegionConnector.DK_ZONE_ID).minusDays(30);
        ZonedDateTime end = start.plusDays(10);
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        String meteringPoint = "meteringPoint";
        PeriodResolutionEnum resolution = PeriodResolutionEnum.PT1H;
        String connectionId = "cid";
        String dataNeedId = "dataNeedId";
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, connectionId, start, end,
                refreshToken, meteringPoint, dataNeedId, resolution, config);
        var state = new EnerginetCustomerSentToPermissionAdministratorState(permissionRequest);
        permissionRequest.changeState(state);

        // When
        assertDoesNotThrow(permissionRequest::invalid);

        // Then
        assertEquals(EnerginetCustomerInvalidState.class, permissionRequest.state().getClass());
    }

    @Test
    void reject_changesToRejectedState() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(EnerginetRegionConnector.DK_ZONE_ID).minusDays(30);
        ZonedDateTime end = start.plusDays(10);
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        String meteringPoint = "meteringPoint";
        PeriodResolutionEnum resolution = PeriodResolutionEnum.PT1H;
        String connectionId = "cid";
        String dataNeedId = "dataNeedId";
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, connectionId, start, end,
                refreshToken, meteringPoint, dataNeedId, resolution, config);
        var state = new EnerginetCustomerSentToPermissionAdministratorState(permissionRequest);
        permissionRequest.changeState(state);

        // When
        assertDoesNotThrow(permissionRequest::rejected);

        // Then
        assertEquals(EnerginetCustomerRejectedState.class, permissionRequest.state().getClass());
    }

    @Test
    void timeOut_notImplemented() {
        // Given
        EnerginetCustomerSentToPermissionAdministratorState state = new EnerginetCustomerSentToPermissionAdministratorState(null);

        // When
        // Then
        assertThrows(UnsupportedOperationException.class, state::timeOut);
    }

    @Test
    void validate_throws() {
        // Given
        EnerginetCustomerSentToPermissionAdministratorState state = new EnerginetCustomerSentToPermissionAdministratorState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        EnerginetCustomerSentToPermissionAdministratorState state = new EnerginetCustomerSentToPermissionAdministratorState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        EnerginetCustomerSentToPermissionAdministratorState state = new EnerginetCustomerSentToPermissionAdministratorState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void terminate_throws() {
        // Given
        EnerginetCustomerSentToPermissionAdministratorState state = new EnerginetCustomerSentToPermissionAdministratorState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::terminate);
    }

    @Test
    void revoke_throws() {
        // Given
        EnerginetCustomerSentToPermissionAdministratorState state = new EnerginetCustomerSentToPermissionAdministratorState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::revoke);
    }

    @Test
    void timeLimit_throws() {
        // Given
        EnerginetCustomerSentToPermissionAdministratorState state = new EnerginetCustomerSentToPermissionAdministratorState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::timeLimit);
    }
}