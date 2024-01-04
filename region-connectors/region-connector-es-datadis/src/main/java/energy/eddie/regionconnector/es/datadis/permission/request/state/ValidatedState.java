package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.api.v0.process.model.states.ValidatedPermissionRequestState;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequest;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidatedState extends ContextualizedPermissionRequestState<EsPermissionRequest> implements ValidatedPermissionRequestState {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatedState.class);
    private final AuthorizationRequest authorizationRequest;
    private final AuthorizationApi authorizationApi;
    private final EsPermissionRequestRepository repository;

    protected ValidatedState(EsPermissionRequest permissionRequest,
                             AuthorizationRequest authorizationRequest,
                             AuthorizationApi authorizationApi, EsPermissionRequestRepository repository) {
        super(permissionRequest);
        this.authorizationRequest = authorizationRequest;
        this.authorizationApi = authorizationApi;
        this.repository = repository;
    }

    @Override
    public void sendToPermissionAdministrator() {
        var response = authorizationApi
                .postAuthorizationRequest(authorizationRequest)
                .doOnSuccess(this::processResponse)
                // TODO should be changed via a transition method with #296
                .doOnError(error -> permissionRequest.changeState(new UnableToSendState(permissionRequest, error)));

        var pendingState = new PendingAcknowledgementState(permissionRequest);
        permissionRequest.changeState(pendingState);
        response.subscribe();
    }

    /**
     * Updates the permissionRequest in accordance with the response received from the PA.
     * Fetches the permissionRequest from the repository instead of using the internal reference, as in the repository,
     * {@link energy.eddie.regionconnector.shared.permission.requests.decorators.MessagingPermissionRequest}s are
     * stored. Using this request from the repository ensures that the {@link energy.eddie.api.v0.ConnectionStatusMessage}
     * is sent when the state changes.
     *
     * @param response Response from the PA.
     */
    private void processResponse(AuthorizationRequestResponse response) {
        var permissionId = permissionRequest.permissionId();
        var optionalPermissionRequest = repository.findByPermissionId(permissionId);
        if (optionalPermissionRequest.isEmpty()) {
            LOGGER.error("Received response for unknown permission request {}", permissionId);
            return;
        }

        var permissionRequest = optionalPermissionRequest.get();
        try {
            permissionRequest.receivedPermissionAdministratorResponse();
            if (response == AuthorizationRequestResponse.NO_SUPPLIES || response == AuthorizationRequestResponse.NO_NIF) {
                permissionRequest.invalid();
            }
        } catch (StateTransitionException e) {
            LOGGER.error("Error changing state of permission request {}", permissionRequest.permissionId(), e);
        }
    }
}
