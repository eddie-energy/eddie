package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.api.v0.process.model.states.ValidatedPermissionRequestState;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class FrEnedisValidatedState
        extends ContextualizedPermissionRequestState<TimeframedPermissionRequest>
        implements ValidatedPermissionRequestState {
    private final EnedisConfiguration configuration;
    private final Context ctx;

    public FrEnedisValidatedState(TimeframedPermissionRequest permissionRequest, EnedisConfiguration configuration, Context ctx) {
        super(permissionRequest);
        this.configuration = configuration;
        this.ctx = ctx;
    }

    @Override
    public void sendToPermissionAdministrator() {
        URI redirectUri;
        try {
            redirectUri = new URIBuilder()
                    .setScheme("https")
                    .setHost("mon-compte-particulier.enedis.fr")
                    .setPath("/dataconnect/v1/oauth2/authorize")
                    .addParameter("client_id", configuration.clientId())
                    .addParameter("response_type", "code")
                    .addParameter("state", permissionRequest.permissionId())
                    .addParameter("duration", "P1Y") // TODO move to config
                    .build();
        } catch (URISyntaxException e) {
            permissionRequest.changeState(new FrEnedisUnableToSendState(permissionRequest, e));
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(Map.of("error", "Unable to create redirect URI"));
            return;
        }
        ctx.json(Map.of("permissionId", permissionRequest.permissionId(), "redirectUri", redirectUri.toString()));
        permissionRequest.changeState(new FrEnedisPendingAcknowledgmentState(permissionRequest));
    }
}