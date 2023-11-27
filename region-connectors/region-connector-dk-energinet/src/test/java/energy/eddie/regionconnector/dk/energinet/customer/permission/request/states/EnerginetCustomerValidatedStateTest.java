package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.SendToPermissionAdministratorException;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.client.EnerginetCustomerApiClient;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.enums.PeriodResolutionEnum;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class EnerginetCustomerValidatedStateTest {
    @Test
    void status_returnsValidated() {
        // Given
        EnerginetCustomerValidatedState state = new EnerginetCustomerValidatedState(null, null);

        // When
        // Then
        assertEquals(PermissionProcessStatus.VALIDATED, state.status());
    }

    @Test
    void validate_throws() {
        // Given
        EnerginetCustomerValidatedState state = new EnerginetCustomerValidatedState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void sendToPermissionAdministrator_changesToSentToPermissionAdministrator() {
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
        EnerginetCustomerApiClient apiClient = mock(EnerginetCustomerApiClient.class);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, connectionId, start, end,
                refreshToken, meteringPoint, dataNeedId, resolution, config);
        var state = new EnerginetCustomerValidatedState(permissionRequest, apiClient);
        permissionRequest.changeState(state);

        // When
        assertDoesNotThrow(permissionRequest::sendToPermissionAdministrator);

        // Then
        assertEquals(EnerginetCustomerPendingAcknowledgmentState.class, permissionRequest.state().getClass());
    }

    @Test
    void givenInvalidRefreshToken_sendToPermissionAdministrator_changesToSentToUnableToSend_andThrows() {
        // Given
        EnerginetCustomerApiClient mockApiClient = mock(EnerginetCustomerApiClient.class);
        var permissionRequest = createPermissionRequestInValidatedState(mockApiClient);

        FeignException.Unauthorized exception = new FeignException.Unauthorized("Foo", mock(Request.class), "foo".getBytes(StandardCharsets.UTF_8), null);
        doThrow(exception).when(mockApiClient).apiToken();


        // When
        SendToPermissionAdministratorException thrown = assertThrows(SendToPermissionAdministratorException.class, permissionRequest::sendToPermissionAdministrator);

        // Then
        assertEquals(EnerginetCustomerUnableToSendState.class, permissionRequest.state().getClass());
        assertTrue(thrown.userFault());
        assertThat(thrown.getMessage()).contains("The given refresh token is not valid");
    }

    @Test
    void givenRateLimit_sendToPermissionAdministrator_changesToSentToUnableToSend_andThrows() {
        // Given
        EnerginetCustomerApiClient mockApiClient = mock(EnerginetCustomerApiClient.class);
        var permissionRequest = createPermissionRequestInValidatedState(mockApiClient);

        FeignException.TooManyRequests exception = new FeignException.TooManyRequests("Foo", mock(Request.class), "foo".getBytes(StandardCharsets.UTF_8), null);
        doThrow(exception).when(mockApiClient).apiToken();


        // When
        SendToPermissionAdministratorException thrown = assertThrows(SendToPermissionAdministratorException.class, permissionRequest::sendToPermissionAdministrator);

        // Then
        assertEquals(EnerginetCustomerUnableToSendState.class, permissionRequest.state().getClass());
        assertFalse(thrown.userFault());
        assertThat(thrown.getMessage()).contains("Energinet is refusing to process the request at the moment, please try again later");
    }

    @Test
    void givenOtherApiError_sendToPermissionAdministrator_changesToSentToUnableToSend_andThrows() {
        // Given
        EnerginetCustomerApiClient mockApiClient = mock(EnerginetCustomerApiClient.class);
        var permissionRequest = createPermissionRequestInValidatedState(mockApiClient);

        FeignException.BadGateway exception = new FeignException.BadGateway("Foo", mock(Request.class), "foo".getBytes(StandardCharsets.UTF_8), null);
        doThrow(exception).when(mockApiClient).apiToken();


        // When
        SendToPermissionAdministratorException thrown = assertThrows(SendToPermissionAdministratorException.class, permissionRequest::sendToPermissionAdministrator);

        // Then
        assertEquals(EnerginetCustomerUnableToSendState.class, permissionRequest.state().getClass());
        assertFalse(thrown.userFault());
        assertThat(thrown.getMessage()).contains("An error occurred, response status from Energinet: ");
    }

    private PermissionRequest createPermissionRequestInValidatedState(EnerginetCustomerApiClient mockApiClient) {
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
        var state = new EnerginetCustomerValidatedState(permissionRequest, mockApiClient);
        permissionRequest.changeState(state);
        return permissionRequest;
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        EnerginetCustomerValidatedState state = new EnerginetCustomerValidatedState(null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void accept_throws() {
        // Given
        EnerginetCustomerValidatedState state = new EnerginetCustomerValidatedState(null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        EnerginetCustomerValidatedState state = new EnerginetCustomerValidatedState(null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        EnerginetCustomerValidatedState state = new EnerginetCustomerValidatedState(null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::reject);
    }

    @Test
    void terminate_throws() {
        // Given
        EnerginetCustomerValidatedState state = new EnerginetCustomerValidatedState(null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::terminate);
    }

    @Test
    void revoke_throws() {
        // Given
        EnerginetCustomerValidatedState state = new EnerginetCustomerValidatedState(null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::revoke);
    }

    @Test
    void timeLimit_throws() {
        // Given
        EnerginetCustomerValidatedState state = new EnerginetCustomerValidatedState(null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::timeLimit);
    }

    @Test
    void timeOut_throws() {
        // Given
        EnerginetCustomerValidatedState state = new EnerginetCustomerValidatedState(null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::timeOut);
    }
}