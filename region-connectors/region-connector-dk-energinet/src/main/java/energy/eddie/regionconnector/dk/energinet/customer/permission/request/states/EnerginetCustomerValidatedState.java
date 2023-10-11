package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.ValidatedPermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.client.EnerginetCustomerApiClient;
import feign.FeignException;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.Map;

public class EnerginetCustomerValidatedState extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest>
        implements ValidatedPermissionRequestState {
    private final String refreshToken;
    private final EnerginetConfiguration configuration;
    private final Context ctx;

    public EnerginetCustomerValidatedState(DkEnerginetCustomerPermissionRequest permissionRequest, String refreshToken, EnerginetConfiguration configuration, Context ctx) {
        super(permissionRequest);
        this.refreshToken = refreshToken;
        this.configuration = configuration;
        this.ctx = ctx;
    }

    @Override
    public void sendToPermissionAdministrator() {
        EnerginetCustomerApiClient apiClient = new EnerginetCustomerApiClient(configuration);
        apiClient.setRefreshToken(refreshToken);

        try {
            apiClient.apiToken();
        } catch (FeignException e) {
            var errorStatus = HttpStatus.forStatus(e.status());

            if (errorStatus.equals(HttpStatus.UNAUTHORIZED)) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(Map.of("error", "The given refresh token is not valid."));
            } else {
                ctx.status(errorStatus);
                ctx.json(Map.of("error", "An error occured."));
            }

            permissionRequest.changeState(new EnerginetCustomerUnableToSendState(permissionRequest, e));
            return;
        }
        ctx.json(Map.of("permissionId", permissionRequest.permissionId()));
        permissionRequest.changeState(new EnerginetCustomerPendingAcknowledgmentState(permissionRequest));
    }
}
