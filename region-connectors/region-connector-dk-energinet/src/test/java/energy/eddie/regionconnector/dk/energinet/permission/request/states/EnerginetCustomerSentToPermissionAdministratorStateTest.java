package energy.eddie.regionconnector.dk.energinet.permission.request.states;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.FutureStateException;
import energy.eddie.api.agnostic.process.model.PastStateException;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EnerginetCustomerSentToPermissionAdministratorStateTest {
    private final ObjectMapper mapper = new DkEnerginetSpringConfig().objectMapper();

    @Test
    void status_returnsSentToPermissionAdministrator() {
        // Given
        EnerginetCustomerSentToPermissionAdministratorState state = new EnerginetCustomerSentToPermissionAdministratorState(
                null,
                new StateBuilderFactory());

        // When
        // Then
        assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, state.status());
    }

    @Test
    void accept_changesToAcceptedState() {
        // Given
        LocalDate start = LocalDate.now(DK_ZONE_ID).minusDays(30);
        LocalDate end = start.plusDays(10);
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        String meteringPoint = "meteringPoint";
        Granularity granularity = Granularity.PT1H;
        String connectionId = "cid";
        String dataNeedId = "dataNeedId";
        EnerginetCustomerApi apiClient = mock(EnerginetCustomerApi.class);
        var forCreation = new PermissionRequestForCreation(connectionId,
                                                           refreshToken,
                                                           meteringPoint,
                                                           dataNeedId);

        StateBuilderFactory factory = new StateBuilderFactory();
        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId,
                                                                       forCreation,
                                                                       apiClient,
                                                                       start,
                                                                       end,
                                                                       granularity,
                                                                       factory,
                                                                       mapper);
        var state = new EnerginetCustomerSentToPermissionAdministratorState(permissionRequest, factory);
        permissionRequest.changeState(state);

        // When
        assertDoesNotThrow(permissionRequest::accept);

        // Then
        assertEquals(EnerginetCustomerAcceptedState.class, permissionRequest.state().getClass());
    }

    @Test
    void invalid_changesToInvalidState() {
        // Given
        LocalDate start = LocalDate.now(DK_ZONE_ID).minusDays(30);
        LocalDate end = start.plusDays(10);
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        String meteringPoint = "meteringPoint";
        Granularity granularity = Granularity.PT1H;
        String connectionId = "cid";
        String dataNeedId = "dataNeedId";
        EnerginetCustomerApi apiClient = mock(EnerginetCustomerApi.class);
        var forCreation = new PermissionRequestForCreation(connectionId,
                                                           refreshToken,
                                                           meteringPoint,
                                                           dataNeedId);

        StateBuilderFactory factory = new StateBuilderFactory();
        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId,
                                                                       forCreation,
                                                                       apiClient,
                                                                       start,
                                                                       end,
                                                                       granularity,
                                                                       factory,
                                                                       mapper);
        var state = new EnerginetCustomerSentToPermissionAdministratorState(permissionRequest, factory);
        permissionRequest.changeState(state);

        // When
        assertDoesNotThrow(permissionRequest::invalid);

        // Then
        assertEquals(EnerginetCustomerInvalidState.class, permissionRequest.state().getClass());
    }

    @Test
    void reject_changesToRejectedState() {
        // Given
        LocalDate start = LocalDate.now(DK_ZONE_ID).minusDays(30);
        LocalDate end = start.plusDays(10);
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        String meteringPoint = "meteringPoint";
        Granularity granularity = Granularity.PT1H;
        String connectionId = "cid";
        String dataNeedId = "dataNeedId";
        EnerginetCustomerApi apiClient = mock(EnerginetCustomerApi.class);
        var forCreation = new PermissionRequestForCreation(connectionId,
                                                           refreshToken,
                                                           meteringPoint,
                                                           dataNeedId);

        StateBuilderFactory factory = new StateBuilderFactory();
        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, forCreation, apiClient,
                                                                       start,
                                                                       end,
                                                                       granularity, factory,
                                                                       mapper);
        var state = new EnerginetCustomerSentToPermissionAdministratorState(permissionRequest, factory);
        permissionRequest.changeState(state);

        // When
        assertDoesNotThrow(permissionRequest::reject);

        // Then
        assertEquals(EnerginetCustomerRejectedState.class, permissionRequest.state().getClass());
    }

    @Test
    void timeOut_notImplemented() {
        // Given
        EnerginetCustomerSentToPermissionAdministratorState state = new EnerginetCustomerSentToPermissionAdministratorState(
                null,
                new StateBuilderFactory());

        // When
        // Then
        assertThrows(UnsupportedOperationException.class, state::timeOut);
    }

    @Test
    void validate_throws() {
        // Given
        EnerginetCustomerSentToPermissionAdministratorState state = new EnerginetCustomerSentToPermissionAdministratorState(
                null,
                new StateBuilderFactory());

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        EnerginetCustomerSentToPermissionAdministratorState state = new EnerginetCustomerSentToPermissionAdministratorState(
                null,
                new StateBuilderFactory());

        // When
        // Then
        assertThrows(PastStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        EnerginetCustomerSentToPermissionAdministratorState state = new EnerginetCustomerSentToPermissionAdministratorState(
                null,
                new StateBuilderFactory());

        // When
        // Then
        assertThrows(PastStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void terminate_throws() {
        // Given
        EnerginetCustomerSentToPermissionAdministratorState state = new EnerginetCustomerSentToPermissionAdministratorState(
                null,
                new StateBuilderFactory());

        // When
        // Then
        assertThrows(FutureStateException.class, state::terminate);
    }

    @Test
    void revoke_throws() {
        // Given
        EnerginetCustomerSentToPermissionAdministratorState state = new EnerginetCustomerSentToPermissionAdministratorState(
                null,
                new StateBuilderFactory());

        // When
        // Then
        assertThrows(FutureStateException.class, state::revoke);
    }

    @Test
    void timeLimit_throws() {
        // Given
        EnerginetCustomerSentToPermissionAdministratorState state = new EnerginetCustomerSentToPermissionAdministratorState(
                null,
                new StateBuilderFactory());

        // When
        // Then
        assertThrows(FutureStateException.class, state::fulfill);
    }
}
