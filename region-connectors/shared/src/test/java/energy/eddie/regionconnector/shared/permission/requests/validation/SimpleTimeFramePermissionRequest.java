package energy.eddie.regionconnector.shared.permission.requests.validation;

import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.v0.DataSourceInformation;

import java.time.LocalDate;
import java.time.ZonedDateTime;

record SimpleTimeFramePermissionRequest(LocalDate start,
                                        LocalDate end) implements TimeframedPermissionRequest {

    @Override
    public String permissionId() {
        return null;
    }

    @Override
    public String connectionId() {
        return null;
    }

    @Override
    public String dataNeedId() {
        return null;
    }

    @Override
    public PermissionRequestState state() {
        return null;
    }

    @Override
    public DataSourceInformation dataSourceInformation() {
        return null;
    }

    @Override
    public ZonedDateTime created() {
        return null;
    }

    @Override
    public void changeState(PermissionRequestState state) {
        // No-Op
    }
}
