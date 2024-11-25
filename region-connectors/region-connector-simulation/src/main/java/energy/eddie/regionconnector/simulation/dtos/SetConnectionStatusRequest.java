package energy.eddie.regionconnector.simulation.dtos;

import energy.eddie.api.v0.PermissionProcessStatus;
import reactor.util.annotation.Nullable;

public class SetConnectionStatusRequest {
    @Nullable
    public String connectionId;
    @Nullable
    public String dataNeedId;
    @Nullable
    public String permissionId;
    @Nullable
    public PermissionProcessStatus connectionStatus;
}