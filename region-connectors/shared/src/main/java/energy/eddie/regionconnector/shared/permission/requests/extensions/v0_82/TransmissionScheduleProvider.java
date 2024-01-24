package energy.eddie.regionconnector.shared.permission.requests.extensions.v0_82;

import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import jakarta.annotation.Nullable;

public interface TransmissionScheduleProvider<T extends TimeframedPermissionRequest> {
    @Nullable
    String findTransmissionSchedule(T permissionRequest);
}
