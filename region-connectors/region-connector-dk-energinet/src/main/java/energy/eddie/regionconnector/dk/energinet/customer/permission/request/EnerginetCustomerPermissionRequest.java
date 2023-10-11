package energy.eddie.regionconnector.dk.energinet.customer.permission.request;

import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.states.EnerginetCustomerCreatedState;
import energy.eddie.regionconnector.dk.energinet.enums.PeriodResolutionEnum;
import io.javalin.http.Context;

import java.time.ZonedDateTime;
import java.util.UUID;

public class EnerginetCustomerPermissionRequest implements DkEnerginetCustomerPermissionRequest {
    public static final String CONNECTION_ID = "connectionId";
    public static final String START_KEY = "start";
    public static final String END_KEY = "end";
    public static final String REFRESH_TOKEN_KEY = "refreshToken";
    public static final String PERIOD_RESOLUTION_KEY = "periodResolution";
    public static final String METERING_POINT_KEY = "meteringPoint";

    private final String permissionId;
    private final String connectionId;
    private final ZonedDateTime start;
    private final ZonedDateTime end;
    private final String refreshToken;
    private final String meteringPoint;
    private final PeriodResolutionEnum periodResolution;
    private PermissionRequestState state;

    public EnerginetCustomerPermissionRequest(String permissionId, String connectionId, Context ctx, EnerginetConfiguration configuration) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.state = new EnerginetCustomerCreatedState(this, ctx, configuration);
        this.start = ctx.formParamAsClass(START_KEY, ZonedDateTime.class).getOrDefault(null);
        this.end = ctx.formParamAsClass(END_KEY, ZonedDateTime.class).getOrDefault(null);
        this.refreshToken = ctx.formParam(REFRESH_TOKEN_KEY);
        this.meteringPoint = ctx.formParam(METERING_POINT_KEY);
        this.periodResolution = ctx.formParamAsClass(PERIOD_RESOLUTION_KEY, PeriodResolutionEnum.class).getOrDefault(null);
    }

    public EnerginetCustomerPermissionRequest(String connectionId, Context ctx, EnerginetConfiguration configuration) {
        this(UUID.randomUUID().toString(), connectionId, ctx, configuration);
    }

    public EnerginetCustomerPermissionRequest(Context ctx, EnerginetConfiguration configuration) {
        this(ctx.formParam(CONNECTION_ID), ctx, configuration);
    }

    @Override
    public String permissionId() {
        return permissionId;
    }

    @Override
    public String connectionId() {
        return connectionId;
    }

    @Override
    public PermissionRequestState state() {
        return state;
    }

    @Override
    public void changeState(PermissionRequestState state) {
        this.state = state;
    }

    @Override
    public ZonedDateTime start() {
        return start;
    }

    @Override
    public ZonedDateTime end() {
        return end;
    }

    @Override
    public String refreshToken() {
        return refreshToken;
    }

    @Override
    public PeriodResolutionEnum periodResolution() {
        return periodResolution;
    }

    @Override
    public String meteringPoint() {
        return meteringPoint;
    }
}
