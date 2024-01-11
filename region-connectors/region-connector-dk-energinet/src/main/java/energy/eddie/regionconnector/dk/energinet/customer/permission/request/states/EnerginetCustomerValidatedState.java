package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.SendToPermissionAdministratorException;
import energy.eddie.api.v0.process.model.states.ValidatedPermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class EnerginetCustomerValidatedState extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest>
        implements ValidatedPermissionRequestState {

    public EnerginetCustomerValidatedState(
            DkEnerginetCustomerPermissionRequest permissionRequest
    ) {
        super(permissionRequest);
    }

    @Override
    public void sendToPermissionAdministrator() throws SendToPermissionAdministratorException {
        try {
            requestAccessToken();
            permissionRequest.changeState(new EnerginetCustomerPendingAcknowledgmentState(permissionRequest));
        } catch (WebClientResponseException.Unauthorized e) {
            permissionRequest.changeState(new EnerginetCustomerUnableToSendState(permissionRequest, e));
            throw new SendToPermissionAdministratorException(this, "The given refresh token is not valid.", true);
        } catch (WebClientResponseException.TooManyRequests e) {
            permissionRequest.changeState(new EnerginetCustomerUnableToSendState(permissionRequest, e));
            throw new SendToPermissionAdministratorException(this, "Energinet is refusing to process the request at the moment, please try again later.", false);
        } catch (Throwable e) {
            permissionRequest.changeState(new EnerginetCustomerUnableToSendState(permissionRequest, e));
            throw new SendToPermissionAdministratorException(this, "An error occurred, with exception " + e, false);
        }
    }

    // We don't know the concrete exception here
    @SuppressWarnings("java:S112")
    private void requestAccessToken() throws Throwable {
        try {
            this.permissionRequest.accessToken().block();
        } catch (RuntimeException e) {
            // Unwrap any thrown checked exceptions
            if (e.getClass().equals(RuntimeException.class)) {
                throw e.getCause();
            } else {
                // Throw the original exception, since it is not wrapped in a runtime exception
                throw e;
            }
        }
    }
}
