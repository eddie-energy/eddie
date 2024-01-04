package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.SendToPermissionAdministratorException;
import energy.eddie.api.v0.process.model.states.ValidatedPermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

public class EnerginetCustomerValidatedState extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest>
        implements ValidatedPermissionRequestState {
    private final EnerginetCustomerApi apiClient;

    public EnerginetCustomerValidatedState(
            DkEnerginetCustomerPermissionRequest permissionRequest,
            EnerginetCustomerApi apiClient
    ) {
        super(permissionRequest);
        this.apiClient = apiClient;
    }

    @Override
    public void sendToPermissionAdministrator() throws SendToPermissionAdministratorException {
        apiClient.setRefreshToken(permissionRequest.refreshToken());

        try {
            apiClient.apiToken();
            permissionRequest.changeState(new EnerginetCustomerPendingAcknowledgmentState(permissionRequest));
        } catch (HttpClientErrorException.Unauthorized e) {
            permissionRequest.changeState(new EnerginetCustomerUnableToSendState(permissionRequest, e));
            throw new SendToPermissionAdministratorException(this, "The given refresh token is not valid", true);
        } catch (HttpClientErrorException.TooManyRequests e) {
            permissionRequest.changeState(new EnerginetCustomerUnableToSendState(permissionRequest, e));
            throw new SendToPermissionAdministratorException(this, "Energinet is refusing to process the request at the moment, please try again later", false);
        } catch (RestClientException e) {
            permissionRequest.changeState(new EnerginetCustomerUnableToSendState(permissionRequest, e));
            throw new SendToPermissionAdministratorException(this, "An error occurred, with exception " + e, false);
        }
    }
}
