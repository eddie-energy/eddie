package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.ValidatedPermissionRequestState;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequest;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestFactory;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
import energy.eddie.regionconnector.es.datadis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

public class ValidatedState extends ContextualizedPermissionRequestState<EsPermissionRequest> implements ValidatedPermissionRequestState {
    private final AuthorizationRequest authorizationRequest;
    private final StateBuilderFactory factory;
    private final AuthorizationApi authorizationApi;

    public ValidatedState(EsPermissionRequest permissionRequest,
                          AuthorizationApi authorizationApi,
                          AuthorizationRequestFactory authorizationRequestFactory,
                          StateBuilderFactory factory) {
        super(permissionRequest);
        this.authorizationApi = authorizationApi;

        this.authorizationRequest = authorizationRequestFactory.fromPermissionRequest(permissionRequest);
        this.factory = factory;
    }


    @Override
    public void sendToPermissionAdministrator() {
        try {
            AuthorizationRequestResponse response = authorizationApi.postAuthorizationRequest(authorizationRequest).block();
            permissionRequest.changeState(
                    factory.create(permissionRequest, PermissionProcessStatus.PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT)
                            .withAuthorizationRequestResponse(response)
                            .build()
            );
        } catch (RuntimeException e) {
            permissionRequest.changeState(new UnableToSendState(permissionRequest, e));
        }
    }
}
