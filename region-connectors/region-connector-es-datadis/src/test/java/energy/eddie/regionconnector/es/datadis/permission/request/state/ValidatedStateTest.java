package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequest;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Sinks;

import java.time.ZonedDateTime;
import java.util.Optional;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class ValidatedStateTest {
    @Mock
    private AuthorizationApi authorizationApi;
    @Mock
    private AuthorizationRequest authorizationRequest;
    @Mock
    private EsPermissionRequestRepository repository;

    @Test
    void sendToPermissionAdministrator_okResponse_changesStateToSentToPA() {
        // Given
        var permissionId = "SomeId";
        var connectionId = "bar";
        var dataNeedId = "luu";
        var nif = "muh";
        var meteringPointId = "kuh";
        var now = ZonedDateTime.now(ZONE_ID_SPAIN);
        var requestDataFrom = now.minusDays(10);
        var requestDataTo = now.minusDays(5);

        var requestForCreation = new PermissionRequestForCreation(connectionId, dataNeedId, nif, meteringPointId,
                requestDataFrom, requestDataTo, MeasurementType.QUARTER_HOURLY);
        var permissionRequest = new DatadisPermissionRequest(permissionId, requestForCreation,
                authorizationApi, repository);
        var validatedState = new ValidatedState(permissionRequest, authorizationRequest, authorizationApi, repository);
        permissionRequest.changeState(validatedState);

        Sinks.One<AuthorizationRequestResponse> responseSink = Sinks.one();
        when(authorizationApi.postAuthorizationRequest(any())).thenReturn(responseSink.asMono());
        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(permissionRequest));

        // When
        assertDoesNotThrow(validatedState::sendToPermissionAdministrator);

        // Then before response is received
        assertEquals(PermissionProcessStatus.PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT, permissionRequest.state().status());
        verify(authorizationApi).postAuthorizationRequest(authorizationRequest);

        // after response is received
        responseSink.tryEmitValue(AuthorizationRequestResponse.OK);
        assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, permissionRequest.state().status());
    }

    @ParameterizedTest
    @EnumSource(
            value = AuthorizationRequestResponse.class,
            names = {"OK"},
            mode = EnumSource.Mode.EXCLUDE)
    void sendToPermissionAdministrator_nonOkResponse_changesStateToInvalid(AuthorizationRequestResponse response) {
        // Given
        var permissionId = "SomeId";
        var connectionId = "bar";
        var dataNeedId = "luu";
        var nif = "muh";
        var meteringPointId = "kuh";
        var now = ZonedDateTime.now(ZONE_ID_SPAIN);
        var requestDataFrom = now.minusDays(10);
        var requestDataTo = now.minusDays(5);

        var requestForCreation = new PermissionRequestForCreation(connectionId, dataNeedId, nif, meteringPointId,
                requestDataFrom, requestDataTo, MeasurementType.QUARTER_HOURLY);
        var permissionRequest = new DatadisPermissionRequest(permissionId, requestForCreation,
                authorizationApi, repository);
        var validatedState = new ValidatedState(permissionRequest, authorizationRequest, authorizationApi, repository);
        permissionRequest.changeState(validatedState);

        Sinks.One<AuthorizationRequestResponse> responseSink = Sinks.one();
        when(authorizationApi.postAuthorizationRequest(any())).thenReturn(responseSink.asMono());
        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(permissionRequest));

        // When
        assertDoesNotThrow(validatedState::sendToPermissionAdministrator);

        // Then before response is received
        assertEquals(PermissionProcessStatus.PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT, permissionRequest.state().status());
        verify(authorizationApi).postAuthorizationRequest(authorizationRequest);

        // after response is received
        responseSink.tryEmitValue(response);
        assertEquals(PermissionProcessStatus.INVALID, permissionRequest.state().status());
    }
}