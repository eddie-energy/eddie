package energy.eddie.regionconnector.dk.energinet.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.SendToPermissionAdministratorException;
import energy.eddie.api.agnostic.process.model.states.ValidatedPermissionRequestState;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class EnerginetCustomerValidatedState extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest>
        implements ValidatedPermissionRequestState {

    private final StateBuilderFactory factory;

    public EnerginetCustomerValidatedState(
            DkEnerginetCustomerPermissionRequest permissionRequest,
            StateBuilderFactory factory) {
        super(permissionRequest);
        this.factory = factory;
    }

    @Override
    public void sendToPermissionAdministrator() throws SendToPermissionAdministratorException {
        try {
            requestAccessToken();
            permissionRequest.changeState(
                    factory.create(permissionRequest, PermissionProcessStatus.PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT).build()
            );
        } catch (WebClientResponseException.Unauthorized | HttpClientErrorException.Unauthorized e) {
            permissionRequest.changeState(
                    factory.create(permissionRequest, PermissionProcessStatus.UNABLE_TO_SEND)
                            .withCause(e)
                            .build()
            );
            throw new SendToPermissionAdministratorException(this, "The given refresh token is not valid.", true);
        } catch (HttpClientErrorException.TooManyRequests e) {
            permissionRequest.changeState(
                    factory.create(permissionRequest, PermissionProcessStatus.UNABLE_TO_SEND)
                            .withCause(e)
                            .build()
            );
            throw new SendToPermissionAdministratorException(this, "Energinet is refusing to process the request at the moment, please try again later.", false);
        } catch (Throwable e) {
            permissionRequest.changeState(
                    factory.create(permissionRequest, PermissionProcessStatus.UNABLE_TO_SEND)
                            .withCause(e)
                            .build()
            );
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