package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.validation.ValidationException;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.config.PlainEnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.enums.PeriodResolutionEnum;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EnerginetCustomerCreatedStateTest {
    @Test
    void validate_changesToValidatedState_whenValid() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneId.of("Europe/Copenhagen")).minusDays(30);
        ZonedDateTime end = start.plusDays(10);
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        String meteringPoint = "meteringPoint";
        PeriodResolutionEnum resolution = PeriodResolutionEnum.PT1H;
        String connectionId = "cid";
        String dataNeedId = "dataNeedId";
        EnerginetConfiguration config = new PlainEnerginetConfiguration("foo:bar", "bloo:too");

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, connectionId, start, end,
                refreshToken, meteringPoint, dataNeedId, resolution, config);

        // When
        assertDoesNotThrow(permissionRequest::validate);

        // Then
        assertEquals(EnerginetCustomerValidatedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenEndBeforeStart() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneId.of("Europe/Copenhagen"));
        var end = start.minusDays(10);
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        String connectionId = "cid";
        String meteringPoint = "meteringPoint";
        PeriodResolutionEnum resolution = PeriodResolutionEnum.PT1H;
        String dataNeedId = "dataNeedId";
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, connectionId,
                start, end, refreshToken, meteringPoint, dataNeedId, resolution, config);

        // When
        var thrown = assertThrows(ValidationException.class, permissionRequest::validate);
        assertThat(thrown.getMessage()).contains("start must be before end");

        // Then
        assertEquals(EnerginetCustomerMalformedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenStartIsOlderThan24Months() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneId.of("Europe/Copenhagen")).minusMonths(30);
        ZonedDateTime end = start.plusDays(1);
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        PeriodResolutionEnum resolution = PeriodResolutionEnum.PT1H;
        String connectionId = "cid";
        String dataNeedId = "dataNeedId";
        String meteringPoint = "meteringPoint";
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, connectionId, start, end,
                refreshToken, meteringPoint, dataNeedId, resolution, config);

        // When
        var thrown = assertThrows(ValidationException.class, permissionRequest::validate);
        assertThat(thrown.getMessage()).contains("start must not be older than");

        // Then
        assertEquals(EnerginetCustomerMalformedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenNotCompletelyInPast() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Copenhagen"));
        ZonedDateTime start = now.minusDays(5);
        ZonedDateTime end = now.plusDays(5);
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        PeriodResolutionEnum resolution = PeriodResolutionEnum.PT1H;
        String connectionId = "cid";
        String dataNeedId = "dataNeedId";
        String meteringPoint = "meteringPoint";
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, connectionId, start, end,
                refreshToken, meteringPoint, dataNeedId, resolution, config);

        // When
        var thrown = assertThrows(ValidationException.class, permissionRequest::validate);
        assertThat(thrown.getMessage()).contains("start and end must be completely in the past");

        // Then
        assertEquals(EnerginetCustomerMalformedState.class, permissionRequest.state().getClass());
    }

    @Test
    void status_returnsCreated() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null, null);

        // When
        // Then
        assertEquals(PermissionProcessStatus.CREATED, state.status());
    }

    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void accept_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::reject);
    }

    @Test
    void terminate_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::terminate);
    }

    @Test
    void revoke_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::revoke);
    }

    @Test
    void timeLimit_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::timeLimit);
    }

    @Test
    void timeOut_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::timeOut);
    }
}