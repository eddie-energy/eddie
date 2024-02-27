package energy.eddie.regionconnector.fr.enedis.permission.request.api;

import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.regionconnector.shared.permission.requests.annotations.InvokeExtensions;

import java.util.Optional;

public interface FrEnedisPermissionRequest extends TimeframedPermissionRequest {
    Optional<String> usagePointId();

    @InvokeExtensions
    void setUsagePointId(String usagePointId);
}