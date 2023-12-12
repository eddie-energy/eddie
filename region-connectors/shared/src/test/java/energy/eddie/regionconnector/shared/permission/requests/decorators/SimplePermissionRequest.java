package energy.eddie.regionconnector.shared.permission.requests.decorators;

import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.PermissionRequestState;

public final class SimplePermissionRequest implements PermissionRequest {
    private final String permissionId;
    private final String connectionId;
    private final String dataNeedId;
    private PermissionRequestState state;

    public SimplePermissionRequest(String permissionId, String connectionId, PermissionRequestState state, String dataNeedId) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.state = state;
        this.dataNeedId = dataNeedId;
    }

    @Override
    public void changeState(PermissionRequestState state) {
        this.state = state;
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
}