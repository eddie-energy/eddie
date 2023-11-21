package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.api.v0.process.model.states.CreatedPermissionRequestState;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.validation.JavalinValidation;

import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest.*;

public class FrEnedisCreatedState
        extends ContextualizedPermissionRequestState<TimeframedPermissionRequest>
        implements CreatedPermissionRequestState {
    private final Context ctx;
    private final EnedisConfiguration configuration;

    public FrEnedisCreatedState(TimeframedPermissionRequest permissionRequest, Context ctx, EnedisConfiguration configuration) {
        super(permissionRequest);
        this.ctx = ctx;
        this.configuration = configuration;
    }

    @Override
    public void validate() {
        var connectionIdValidator = ctx.formParamAsClass(CONNECTION_ID, String.class)
                .check(s -> !s.isBlank(), "connectionId must not be blank");

        var startValidator = ctx.formParamAsClass(START_KEY, ZonedDateTime.class);
        var endValidator = ctx.formParamAsClass(END_KEY, ZonedDateTime.class)
                .check(end -> end.isAfter(startValidator.get()), "end must be after start");
        var errors = JavalinValidation.collectErrors(connectionIdValidator, startValidator, endValidator);
        if (!errors.isEmpty()) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(errors);
            permissionRequest.changeState(new FrEnedisMalformedState(permissionRequest, errors));
            return;
        }
        permissionRequest.changeState(new FrEnedisValidatedState(permissionRequest, configuration, ctx));
    }
}