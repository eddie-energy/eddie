package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.validation.ValidationException;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EnerginetCustomerCreatedStateTest {
    @Test
    void validate_changesToValidatedState_whenValid() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(EnerginetRegionConnector.DK_ZONE_ID).minusDays(30);
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        String meteringPoint = "meteringPoint";
        Granularity granularity = Granularity.PT1H;
        String connectionId = "cid";
        String dataNeedId = "dataNeedId";
        EnerginetCustomerApi apiClient = mock(EnerginetCustomerApi.class);
        var forCreation = new PermissionRequestForCreation(connectionId, start, start.plusDays(5), refreshToken, granularity, meteringPoint, dataNeedId);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, forCreation, apiClient);

        // When
        assertDoesNotThrow(permissionRequest::validate);

        // Then
        assertEquals(EnerginetCustomerValidatedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenEndBeforeStart() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(EnerginetRegionConnector.DK_ZONE_ID);
        var end = start.minusDays(10);
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        String connectionId = "cid";
        String meteringPoint = "meteringPoint";
        Granularity granularity = Granularity.PT1H;
        String dataNeedId = "dataNeedId";
        EnerginetCustomerApi apiClient = mock(EnerginetCustomerApi.class);
        var forCreation = new PermissionRequestForCreation(connectionId, start, end, refreshToken, granularity, meteringPoint, dataNeedId);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, forCreation, apiClient);

        // When
        var thrown = assertThrows(ValidationException.class, permissionRequest::validate);
        assertThat(thrown.getMessage()).contains("start must be before or equal to end");

        // Then
        assertEquals(EnerginetCustomerMalformedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenStartIsOlderThan24Months() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(EnerginetRegionConnector.DK_ZONE_ID).minusMonths(30);
        ZonedDateTime end = start.plusDays(1);
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        Granularity granularity = Granularity.PT1H;
        String connectionId = "cid";
        String dataNeedId = "dataNeedId";
        String meteringPoint = "meteringPoint";
        EnerginetCustomerApi apiClient = mock(EnerginetCustomerApi.class);
        var forCreation = new PermissionRequestForCreation(connectionId, start, end, refreshToken, granularity, meteringPoint, dataNeedId);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, forCreation, apiClient);

        // When
        var thrown = assertThrows(ValidationException.class, permissionRequest::validate);
        assertThat(thrown.getMessage()).contains("start must not be older than");

        // Then
        assertEquals(EnerginetCustomerMalformedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenNotCompletelyInPast() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(EnerginetRegionConnector.DK_ZONE_ID);
        ZonedDateTime start = now.minusDays(5);
        ZonedDateTime end = now.plusDays(5);
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        Granularity granularity = Granularity.PT1H;
        String connectionId = "cid";
        String dataNeedId = "dataNeedId";
        String meteringPoint = "meteringPoint";
        EnerginetCustomerApi apiClient = mock(EnerginetCustomerApi.class);
        var forCreation = new PermissionRequestForCreation(connectionId, start, end, refreshToken, granularity, meteringPoint, dataNeedId);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, forCreation, apiClient);

        // When
        var thrown = assertThrows(ValidationException.class, permissionRequest::validate);
        assertThat(thrown.getMessage()).contains("start and end must be completely in the past");

        // Then
        assertEquals(EnerginetCustomerMalformedState.class, permissionRequest.state().getClass());
    }

    @Test
    void status_returnsCreated() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null);

        // When
        // Then
        assertEquals(PermissionProcessStatus.CREATED, state.status());
    }

    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void accept_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::reject);
    }

    @Test
    void terminate_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::terminate);
    }

    @Test
    void revoke_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::revoke);
    }

    @Test
    void timeLimit_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::timeLimit);
    }

    @Test
    void timeOut_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::timeOut);
    }
}