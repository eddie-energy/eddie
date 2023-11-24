package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.SendToPermissionAdministratorException;
import energy.eddie.api.v0.process.model.states.ValidatedPermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.customer.client.EnerginetCustomerApiClient;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;
import feign.FeignException;
import org.springframework.http.HttpStatus;

public class EnerginetCustomerValidatedState extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest>
        implements ValidatedPermissionRequestState {
    private final EnerginetCustomerApiClient apiClient;

    public EnerginetCustomerValidatedState(
            DkEnerginetCustomerPermissionRequest permissionRequest,
            EnerginetCustomerApiClient apiClient) {
        super(permissionRequest);
        this.apiClient = apiClient;
    }

    @Override
    public void sendToPermissionAdministrator() throws SendToPermissionAdministratorException {
        apiClient.setRefreshToken(permissionRequest.refreshToken());

        try {
            apiClient.apiToken();
        } catch (FeignException e) {
            var errorStatus = HttpStatus.resolve(e.status());

            permissionRequest.changeState(new EnerginetCustomerUnableToSendState(permissionRequest, e));

            if (errorStatus == HttpStatus.UNAUTHORIZED) {
                throw new SendToPermissionAdministratorException(this, "The given refresh token is not valid", true);
            } else if (errorStatus == HttpStatus.TOO_MANY_REQUESTS) {
                throw new SendToPermissionAdministratorException(this, "Energinet is refusing to process the request at the moment, please try again later", false);
            } else {
                throw new SendToPermissionAdministratorException(this, "An error occurred, response status from Energinet: " + errorStatus, false);
            }
        }
        permissionRequest.changeState(new EnerginetCustomerPendingAcknowledgmentState(permissionRequest));
    }
}
