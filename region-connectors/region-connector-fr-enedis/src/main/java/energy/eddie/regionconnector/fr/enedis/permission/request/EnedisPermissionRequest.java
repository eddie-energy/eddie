package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.permission.request.states.FrEnedisCreatedState;
import io.javalin.http.Context;

import java.time.ZonedDateTime;
import java.util.UUID;

public class EnedisPermissionRequest implements TimeframedPermissionRequest {
    public static final String CONNECTION_ID = "connectionId";
    public static final String START_KEY = "start";
    public static final String END_KEY = "end";
    private static final String DATA_NEED_ID = "dataNeedId";
    private final String permissionId;
    private final String connectionId;
    private final ZonedDateTime start;
    private final ZonedDateTime end;
    private final String dataNeedId;
    private PermissionRequestState state;

    public EnedisPermissionRequest(String permissionId, String connectionId, Context ctx, EnedisConfiguration configuration) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.state = new FrEnedisCreatedState(this, ctx, configuration);
        this.dataNeedId = ctx.formParam(DATA_NEED_ID);
        this.start = ctx.formParamAsClass(START_KEY, ZonedDateTime.class).getOrDefault(null);
        this.end = ctx.formParamAsClass(END_KEY, ZonedDateTime.class).getOrDefault(null);
    }

    public EnedisPermissionRequest(String connectionId, Context ctx, EnedisConfiguration configuration) {
        this(UUID.randomUUID().toString(), connectionId, ctx, configuration);
    }

    public EnedisPermissionRequest(Context ctx, EnedisConfiguration configuration) {
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
    public String dataNeedId() {
        return dataNeedId;
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
}