package energy.eddie.regionconnector.es.datadis.permission.request.state;


import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.CreatedPermissionRequestState;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationResponseHandler;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.validation.JavalinValidation;

import java.time.ZonedDateTime;
import java.util.List;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.MAXIMUM_MONTHS_IN_THE_PAST;
import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static energy.eddie.regionconnector.es.datadis.utils.ParameterKeys.*;

public class CreatedState extends ContextualizedPermissionRequestState<EsPermissionRequest> implements CreatedPermissionRequestState {

    private final AuthorizationApi authorizationApi;
    private final Context ctx;
    private final AuthorizationResponseHandler authorizationResponseHandler;

    public CreatedState(EsPermissionRequest permissionRequest, Context ctx, AuthorizationApi authorizationApi, AuthorizationResponseHandler authorizationResponseHandler) {
        super(permissionRequest);
        this.authorizationApi = authorizationApi;
        this.ctx = ctx;
        this.authorizationResponseHandler = authorizationResponseHandler;
    }


    @Override
    public void validate() {
        var connectionIdValidator = ctx.formParamAsClass(CONNECTION_ID_KEY, String.class).check(s -> s != null && !s.isBlank(), "connectionId must not be null or blank");
        var nifValidator = ctx.formParamAsClass(NIF_KEY, String.class).check(s -> s != null && !s.isBlank(), "nif must not be null or blank");
        var meteringPointIdValidator = ctx.formParamAsClass(METERING_POINT_ID_KEY, String.class).check(s -> s != null && !s.isBlank(), "meteringPointId must not be null or blank");

        var now = ZonedDateTime.now(ZONE_ID_SPAIN);
        var startValidator = ctx.formParamAsClass(REQUEST_DATE_FROM_KEY, ZonedDateTime.class)
                .check(start -> start.isAfter(now.minusMonths(MAXIMUM_MONTHS_IN_THE_PAST)), "start must not be older than " + MAXIMUM_MONTHS_IN_THE_PAST + " months");

        var endValidator = ctx.formParamAsClass(REQUEST_DATA_TO_KEY, ZonedDateTime.class)
                .check(end -> !startValidator.errors().isEmpty() || end.isAfter(startValidator.get()), "end must be after start")
                .check(end -> end.isBefore(now.minusDays(1)), "end must be in the past"); // for now, we only support historical data

        var measurementTypeValidator = ctx.formParamAsClass(MEASUREMENT_TYPE_KEY, MeasurementType.class);

        var errors = JavalinValidation.collectErrors(
                nifValidator,
                connectionIdValidator,
                meteringPointIdValidator,
                startValidator,
                endValidator,
                measurementTypeValidator
        );
        if (!errors.isEmpty()) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(errors);
            permissionRequest.changeState(new MalformedState(permissionRequest, errors));
            return;
        }
        AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                permissionRequest.permissionStart().toLocalDate(),
                permissionRequest.permissionEnd().toLocalDate(),
                permissionRequest.nif(),
                List.of(permissionRequest.meteringPointId())
        );

        permissionRequest.changeState(new ValidatedState(permissionRequest, authorizationRequest, authorizationApi, ctx, authorizationResponseHandler));
    }

    @Override
    public void terminate() {
        throw new IllegalStateException("Not implemented yet");
    }

}
