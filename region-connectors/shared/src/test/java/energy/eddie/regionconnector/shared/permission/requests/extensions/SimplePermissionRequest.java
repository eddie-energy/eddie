package energy.eddie.regionconnector.shared.permission.requests.extensions;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.DataSourceInformation;

import java.time.LocalDate;
import java.time.ZonedDateTime;

public final class SimplePermissionRequest implements PermissionRequest {
    private final String permissionId;
    private final String connectionId;
    private final String dataNeedId;
    private final LocalDate start;
    private final LocalDate end;
    private final ZonedDateTime created;
    private PermissionRequestState state;

    public SimplePermissionRequest(
            String permissionId,
            String connectionId,
            PermissionRequestState state,
            String dataNeedId
    ) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.state = state;
        this.dataNeedId = dataNeedId;
        start = null;
        end = null;
        created = null;
    }

    public SimplePermissionRequest(
            String permissionId,
            String connectionId,
            PermissionRequestState state,
            String dataNeedId,
            LocalDate start,
            LocalDate end,
            ZonedDateTime created
    ) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.state = state;
        this.dataNeedId = dataNeedId;
        this.start = start;
        this.end = end;
        this.created = created;
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
        return new DummyDataSourceInformation();
    }

    @Override
    public ZonedDateTime created() {
        return created;
    }

    @Override
    public LocalDate start() {
        return start;
    }

    @Override
    public LocalDate end() {
        return end;
    }

    @Override
    public void changeState(PermissionRequestState state) {
        this.state = state;
    }
}
