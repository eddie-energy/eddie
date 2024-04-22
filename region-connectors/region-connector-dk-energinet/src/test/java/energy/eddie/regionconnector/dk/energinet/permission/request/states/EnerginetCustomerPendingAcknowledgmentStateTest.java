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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class EnerginetCustomerPendingAcknowledgmentStateTest {
    private final ObjectMapper mapper = new DkEnerginetSpringConfig().objectMapper();
    @Test
    void status_returnsPendingAcknowledgement() {
        // Given
        StateBuilderFactory factory = new StateBuilderFactory();
        EnerginetCustomerPendingAcknowledgmentState state = new EnerginetCustomerPendingAcknowledgmentState(null,
                                                                                                            factory);

        // When
        // Then
        assertEquals(PermissionProcessStatus.PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT, state.status());
    }

    @Test
    void receivedPermissionAdminAnswer_transitionsState() {
        // Given
        var start = LocalDate.now(DK_ZONE_ID).minusDays(10);
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        String meteringPoint = "meteringPoint";
        Granularity granularity = Granularity.PT1H;
        String connectionId = "cid";
        String dataNeedId = "dataNeedId";
        EnerginetCustomerApi apiClient = mock(EnerginetCustomerApi.class);
        StateBuilderFactory factory = new StateBuilderFactory();
        var forCreation = new PermissionRequestForCreation(connectionId,
                                                           refreshToken,
                                                           meteringPoint,
                                                           dataNeedId);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId,
                                                                       forCreation,
                                                                       apiClient,
                                                                       start,
                                                                       start.plusDays(5),
                                                                       granularity,
                                                                       factory,
                                                                       mapper);
        EnerginetCustomerPendingAcknowledgmentState state = new EnerginetCustomerPendingAcknowledgmentState(
                permissionRequest,
                factory);

        // When
        state.receivedPermissionAdministratorResponse();

        // Then
        assertEquals(EnerginetCustomerSentToPermissionAdministratorState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_throws() {
        // Given
        StateBuilderFactory factory = new StateBuilderFactory();
        EnerginetCustomerPendingAcknowledgmentState state = new EnerginetCustomerPendingAcknowledgmentState(null,
                                                                                                            factory);

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        StateBuilderFactory factory = new StateBuilderFactory();
        EnerginetCustomerPendingAcknowledgmentState state = new EnerginetCustomerPendingAcknowledgmentState(null,
                                                                                                            factory);

        // When
        // Then
        assertThrows(PastStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void accept_throws() {
        // Given
        StateBuilderFactory factory = new StateBuilderFactory();
        EnerginetCustomerPendingAcknowledgmentState state = new EnerginetCustomerPendingAcknowledgmentState(null,
                                                                                                            factory);

        // When
        // Then
        assertThrows(FutureStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        StateBuilderFactory factory = new StateBuilderFactory();
        EnerginetCustomerPendingAcknowledgmentState state = new EnerginetCustomerPendingAcknowledgmentState(null,
                                                                                                            factory);

        // When
        // Then
        assertThrows(FutureStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        StateBuilderFactory factory = new StateBuilderFactory();
        EnerginetCustomerPendingAcknowledgmentState state = new EnerginetCustomerPendingAcknowledgmentState(null,
                                                                                                            factory);

        // When
        // Then
        assertThrows(FutureStateException.class, state::reject);
    }

    @Test
    void terminate_throws() {
        // Given
        StateBuilderFactory factory = new StateBuilderFactory();
        EnerginetCustomerPendingAcknowledgmentState state = new EnerginetCustomerPendingAcknowledgmentState(null,
                                                                                                            factory);

        // When
        // Then
        assertThrows(FutureStateException.class, state::terminate);
    }

    @Test
    void revoke_throws() {
        // Given
        StateBuilderFactory factory = new StateBuilderFactory();
        EnerginetCustomerPendingAcknowledgmentState state = new EnerginetCustomerPendingAcknowledgmentState(null,
                                                                                                            factory);

        // When
        // Then
        assertThrows(FutureStateException.class, state::revoke);
    }

    @Test
    void timeLimit_throws() {
        // Given
        StateBuilderFactory factory = new StateBuilderFactory();
        EnerginetCustomerPendingAcknowledgmentState state = new EnerginetCustomerPendingAcknowledgmentState(null,
                                                                                                            factory);

        // When
        // Then
        assertThrows(FutureStateException.class, state::fulfill);
    }

    @Test
    void timeOut_throws() {
        // Given
        StateBuilderFactory factory = new StateBuilderFactory();
        EnerginetCustomerPendingAcknowledgmentState state = new EnerginetCustomerPendingAcknowledgmentState(null,
                                                                                                            factory);

        // When
        // Then
        assertThrows(FutureStateException.class, state::timeOut);
    }
}
