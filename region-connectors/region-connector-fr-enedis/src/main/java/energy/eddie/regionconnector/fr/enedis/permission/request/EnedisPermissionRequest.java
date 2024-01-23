package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.regionconnector.fr.enedis.permission.request.states.FrEnedisCreatedState;

import java.time.ZonedDateTime;
import java.util.UUID;

public class EnedisPermissionRequest implements TimeframedPermissionRequest {
    private static final EnedisDataSourceInformation dataSourceInformation = new EnedisDataSourceInformation();
    private final String permissionId;
    private final String connectionId;
    private final ZonedDateTime start;
    private final ZonedDateTime end;
    private final String dataNeedId;
    private PermissionRequestState state;

    public EnedisPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            ZonedDateTime start,
            ZonedDateTime end
    ) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.state = new FrEnedisCreatedState(this);
        this.dataNeedId = dataNeedId;
        this.start = start;
        this.end = end;
    }

    public EnedisPermissionRequest(
            String connectionId,
            String dataNeedId,
            ZonedDateTime start,
            ZonedDateTime end
    ) {
        this(UUID.randomUUID().toString(), connectionId, dataNeedId, start, end);
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
    public DataSourceInformation dataSourceInformation() {
        return dataSourceInformation;
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