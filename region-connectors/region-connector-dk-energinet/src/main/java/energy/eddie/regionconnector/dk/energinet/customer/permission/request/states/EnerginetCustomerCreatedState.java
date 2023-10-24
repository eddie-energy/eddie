package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.CreatedPermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.enums.PeriodResolutionEnum;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.validation.JavalinValidation;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.dk.energinet.customer.permission.request.EnerginetCustomerPermissionRequest.*;

public class EnerginetCustomerCreatedState extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest>
        implements CreatedPermissionRequestState {
    private final Context ctx;
    private final EnerginetConfiguration configuration;

    public EnerginetCustomerCreatedState(DkEnerginetCustomerPermissionRequest permissionRequest, Context ctx, EnerginetConfiguration configuration) {
        super(permissionRequest);
        this.ctx = ctx;
        this.configuration = configuration;
    }

    @Override
    public void validate() {
        var connectionIdValidator = ctx.formParamAsClass(CONNECTION_ID, String.class)
                .check(s -> !s.isBlank(), "connectionId must not be blank");
        var refreshTokenValidator = ctx.formParamAsClass(REFRESH_TOKEN_KEY, String.class)
                .check(s -> !s.isBlank(), "refreshToken must not be blank");
        var periodResolutionValidator = ctx.formParamAsClass(PERIOD_RESOLUTION_KEY, PeriodResolutionEnum.class);
        var meteringPointValidator = ctx.formParamAsClass(METERING_POINT_KEY, String.class)
                .check(s -> !s.isBlank(), "meteringPoint must not be blank");

        var now = ZonedDateTime.now(ZoneId.of("Europe/Copenhagen"));
        var startValidator = ctx.formParamAsClass(START_KEY, ZonedDateTime.class)
                .check(start -> start.isAfter(now.minusMonths(24)), "start must not be older than " + 24 + " months");
        var endValidator = ctx.formParamAsClass(END_KEY, ZonedDateTime.class)
                .check(end -> !startValidator.errors().isEmpty() || end.isAfter(startValidator.get()), "end must be after start")
                .check(end -> end.isBefore(now.minusDays(1)), "end must be in the past"); // for now, we only support historical data

        var errors = JavalinValidation.collectErrors(
                connectionIdValidator,
                refreshTokenValidator,
                periodResolutionValidator,
                meteringPointValidator,
                startValidator,
                endValidator
        );
        if (!errors.isEmpty()) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(errors);
            permissionRequest.changeState(new EnerginetCustomerMalformedState(permissionRequest, errors));
            return;
        }

        permissionRequest.changeState(new EnerginetCustomerValidatedState(permissionRequest, refreshTokenValidator.get(), configuration, ctx));
    }
}
