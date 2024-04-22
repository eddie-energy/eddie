package energy.eddie.regionconnector.dk.energinet.permission.request.states;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.FutureStateException;
import energy.eddie.api.agnostic.process.model.validation.ValidationException;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.DkEnerginetSpringConfig;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.StateBuilderFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EnerginetCustomerCreatedStateTest {
    @Test
    void validate_changesToValidatedState_whenValid() {
        // Given
        LocalDate start = LocalDate.now(DK_ZONE_ID).minusDays(30);
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        String meteringPoint = "meteringPoint";
        Granularity granularity = Granularity.PT1H;
        String connectionId = "cid";
        String dataNeedId = "dataNeedId";
        EnerginetCustomerApi apiClient = mock(EnerginetCustomerApi.class);
        var forCreation = new PermissionRequestForCreation(connectionId, refreshToken, meteringPoint, dataNeedId);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId,
                                                                       forCreation,
                                                                       apiClient,
                                                                       start,
                                                                       start.plusDays(5),
                                                                       granularity,
                                                                       new StateBuilderFactory(),
                                                                       new DkEnerginetSpringConfig().objectMapper());

        // When
        assertDoesNotThrow(permissionRequest::validate);

        // Then
        assertEquals(EnerginetCustomerValidatedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenEndBeforeStart() {
        // Given
        LocalDate start = LocalDate.now(DK_ZONE_ID);
        var end = start.minusDays(10);
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        String connectionId = "cid";
        String meteringPoint = "meteringPoint";
        Granularity granularity = Granularity.PT1H;
        String dataNeedId = "dataNeedId";
        EnerginetCustomerApi apiClient = mock(EnerginetCustomerApi.class);
        var forCreation = new PermissionRequestForCreation(connectionId,
                                                           refreshToken,
                                                           meteringPoint,
                                                           dataNeedId);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId,
                                                                       forCreation,
                                                                       apiClient,
                                                                       start,
                                                                       end,
                                                                       granularity,
                                                                       new StateBuilderFactory(),
                                                                       new DkEnerginetSpringConfig().objectMapper());

        // When
        var thrown = assertThrows(ValidationException.class, permissionRequest::validate);
        assertThat(thrown.getMessage()).contains("start must be before or equal to end");

        // Then
        assertEquals(EnerginetCustomerMalformedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenStartIsOlderThan24Months() {
        // Given
        LocalDate start = LocalDate.now(DK_ZONE_ID).minusMonths(30);
        LocalDate end = start.plusDays(1);
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        Granularity granularity = Granularity.PT1H;
        String connectionId = "cid";
        String dataNeedId = "dataNeedId";
        String meteringPoint = "meteringPoint";
        EnerginetCustomerApi apiClient = mock(EnerginetCustomerApi.class);
        var forCreation = new PermissionRequestForCreation(connectionId,
                                                           refreshToken,
                                                           meteringPoint,
                                                           dataNeedId);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId,
                                                                       forCreation,
                                                                       apiClient,
                                                                       start,
                                                                       end,
                                                                       granularity,
                                                                       new StateBuilderFactory(),
                                                                       new DkEnerginetSpringConfig().objectMapper());

        // When
        var thrown = assertThrows(ValidationException.class, permissionRequest::validate);
        assertThat(thrown.getMessage()).contains("start must not be older than");

        // Then
        assertEquals(EnerginetCustomerMalformedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenNotCompletelyInPast() {
        // Given
        LocalDate now = LocalDate.now(DK_ZONE_ID);
        LocalDate start = now.minusDays(5);
        LocalDate end = now.plusDays(5);
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        Granularity granularity = Granularity.PT1H;
        String connectionId = "cid";
        String dataNeedId = "dataNeedId";
        String meteringPoint = "meteringPoint";
        EnerginetCustomerApi apiClient = mock(EnerginetCustomerApi.class);
        var forCreation = new PermissionRequestForCreation(connectionId,
                                                           refreshToken,
                                                           meteringPoint,
                                                           dataNeedId);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId,
                                                                       forCreation,
                                                                       apiClient,
                                                                       start,
                                                                       end,
                                                                       granularity,
                                                                       new StateBuilderFactory(),
                                                                       new DkEnerginetSpringConfig().objectMapper());

        // When
        var thrown = assertThrows(ValidationException.class, permissionRequest::validate);
        assertThat(thrown.getMessage()).contains(
                "start and end must lie completely in the past or completely in the future");

        // Then
        assertEquals(EnerginetCustomerMalformedState.class, permissionRequest.state().getClass());
    }

    @Test
    void status_returnsCreated() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null, new StateBuilderFactory());

        // When
        // Then
        assertEquals(PermissionProcessStatus.CREATED, state.status());
    }

    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null, new StateBuilderFactory());

        // When
        // Then
        assertThrows(FutureStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null, new StateBuilderFactory());

        // When
        // Then
        assertThrows(FutureStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void accept_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null, new StateBuilderFactory());

        // When
        // Then
        assertThrows(FutureStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null, new StateBuilderFactory());

        // When
        // Then
        assertThrows(FutureStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null, new StateBuilderFactory());

        // When
        // Then
        assertThrows(FutureStateException.class, state::reject);
    }

    @Test
    void terminate_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null, new StateBuilderFactory());

        // When
        // Then
        assertThrows(FutureStateException.class, state::terminate);
    }

    @Test
    void revoke_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null, new StateBuilderFactory());

        // When
        // Then
        assertThrows(FutureStateException.class, state::revoke);
    }

    @Test
    void timeLimit_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null, new StateBuilderFactory());

        // When
        // Then
        assertThrows(FutureStateException.class, state::fulfill);
    }

    @Test
    void timeOut_throws() {
        // Given
        EnerginetCustomerCreatedState state = new EnerginetCustomerCreatedState(null, new StateBuilderFactory());

        // When
        // Then
        assertThrows(FutureStateException.class, state::timeOut);
    }
}
