package energy.eddie.regionconnector.shared.cim.v0_82;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import jakarta.annotation.Nullable;

public interface TransmissionScheduleProvider<T extends PermissionRequest> {
    @Nullable
    String findTransmissionSchedule(T permissionRequest);
}
