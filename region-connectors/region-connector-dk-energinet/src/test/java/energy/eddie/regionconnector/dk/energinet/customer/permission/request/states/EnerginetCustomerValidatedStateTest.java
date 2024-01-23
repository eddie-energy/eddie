package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.FutureStateException;
import energy.eddie.api.agnostic.process.model.PastStateException;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.SendToPermissionAdministratorException;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.client.EnerginetCustomerApiClient;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class EnerginetCustomerValidatedStateTest {
    @Test
    void status_returnsValidated() {
        // Given
        EnerginetCustomerValidatedState state = new EnerginetCustomerValidatedState(null);

        // When
        // Then
        assertEquals(PermissionProcessStatus.VALIDATED, state.status());
    }

    @Test
    void validate_throws() {
        // Given
        EnerginetCustomerValidatedState state = new EnerginetCustomerValidatedState(null);

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
        Granularity granularity = Granularity.PT1H;
        String connectionId = "cid";
        String dataNeedId = "dataNeedId";
        EnerginetCustomerApi apiClient = mock(EnerginetCustomerApi.class);
        doReturn(Mono.just("token")).when(apiClient).accessToken(anyString());
        var forCreation = new PermissionRequestForCreation(connectionId, start, end, refreshToken, granularity, meteringPoint, dataNeedId);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, forCreation, apiClient);
        var state = new EnerginetCustomerValidatedState(permissionRequest);
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


        RestClientException exception = HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Foo", HttpHeaders.EMPTY, "foo".getBytes(StandardCharsets.UTF_8), null);
        doReturn(Mono.error(exception)).when(mockApiClient).accessToken(anyString());


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
        RestClientException exception = HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Foo", HttpHeaders.EMPTY, "foo".getBytes(StandardCharsets.UTF_8), null);
        EnerginetCustomerApiClient mockApiClient = mock(EnerginetCustomerApiClient.class);
        doReturn(Mono.error(exception)).when(mockApiClient).accessToken(anyString());
        var permissionRequest = createPermissionRequestInValidatedState(mockApiClient);

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

        RestClientException exception = HttpClientErrorException.create(HttpStatus.BAD_GATEWAY, "Foo", HttpHeaders.EMPTY, "foo".getBytes(StandardCharsets.UTF_8), null);
        doReturn(Mono.error(exception)).when(mockApiClient).accessToken(anyString());


        // When
        SendToPermissionAdministratorException thrown = assertThrows(SendToPermissionAdministratorException.class, permissionRequest::sendToPermissionAdministrator);

        // Then
        assertEquals(EnerginetCustomerUnableToSendState.class, permissionRequest.state().getClass());
        assertFalse(thrown.userFault());
        assertThat(thrown.getMessage()).contains("An error occurred, with exception ");
    }

    private PermissionRequest createPermissionRequestInValidatedState(EnerginetCustomerApi apiClient) {
        ZonedDateTime start = ZonedDateTime.now(EnerginetRegionConnector.DK_ZONE_ID).minusDays(30);
        ZonedDateTime end = start.plusDays(10);
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        String meteringPoint = "meteringPoint";
        Granularity granularity = Granularity.PT1H;
        String connectionId = "cid";
        String dataNeedId = "dataNeedId";
        var forCreation = new PermissionRequestForCreation(connectionId, start, end, refreshToken, granularity, meteringPoint, dataNeedId);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, forCreation, apiClient);
        var state = new EnerginetCustomerValidatedState(permissionRequest);
        permissionRequest.changeState(state);
        return permissionRequest;
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        EnerginetCustomerValidatedState state = new EnerginetCustomerValidatedState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void accept_throws() {
        // Given
        EnerginetCustomerValidatedState state = new EnerginetCustomerValidatedState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        EnerginetCustomerValidatedState state = new EnerginetCustomerValidatedState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        EnerginetCustomerValidatedState state = new EnerginetCustomerValidatedState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::reject);
    }

    @Test
    void terminate_throws() {
        // Given
        EnerginetCustomerValidatedState state = new EnerginetCustomerValidatedState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::terminate);
    }

    @Test
    void revoke_throws() {
        // Given
        EnerginetCustomerValidatedState state = new EnerginetCustomerValidatedState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::revoke);
    }

    @Test
    void timeLimit_throws() {
        // Given
        EnerginetCustomerValidatedState state = new EnerginetCustomerValidatedState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::fulfill);
    }

    @Test
    void timeOut_throws() {
        // Given
        EnerginetCustomerValidatedState state = new EnerginetCustomerValidatedState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::timeOut);
    }
}