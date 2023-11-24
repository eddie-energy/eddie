package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.api.v0.process.model.states.ValidatedPermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.client.EnerginetCustomerApiClient;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;
import feign.FeignException;
import org.springframework.http.HttpStatus;

public class EnerginetCustomerValidatedState extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest>
        implements ValidatedPermissionRequestState {
    private final EnerginetConfiguration configuration;

    public EnerginetCustomerValidatedState(
            DkEnerginetCustomerPermissionRequest permissionRequest,
            EnerginetConfiguration configuration) {
        super(permissionRequest);
        this.configuration = configuration;
    }

    @Override
    public void sendToPermissionAdministrator() throws StateTransitionException {
        EnerginetCustomerApiClient apiClient = new EnerginetCustomerApiClient(configuration);
        apiClient.setRefreshToken(permissionRequest.refreshToken());

        try {
            apiClient.apiToken();
        } catch (FeignException e) {
            var errorStatus = HttpStatus.resolve(e.status());

            permissionRequest.changeState(new EnerginetCustomerUnableToSendState(permissionRequest, e));

            if (errorStatus == HttpStatus.UNAUTHORIZED) {
                // TODO: this is not like how the process model is defined...
                // The given refresh token for the API is not valid -> therefore no consent was given
//                permissionRequest.receivedPermissionAdministratorResponse();
//                permissionRequest.rejected();
                // TODO "Exceptions should only be used for exceptional cases" - how to notify controller about this then?
                // passing a responseEntity doesn't make sense either because then it's not separated anymore
//                throw new SendToPermissionAdministratorFailedException(this, "The given refresh token is not valid.", true);
            } else {
//                throw new SendToPermissionAdministratorFailedException(this, "An error occurred, status: " + errorStatus, false);
            }
        }
        permissionRequest.changeState(new EnerginetCustomerPendingAcknowledgmentState(permissionRequest));
    }
}
