package energy.eddie.regionconnector.fr.enedis.permission.request.api;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.shared.permission.requests.annotations.InvokeExtensions;

import java.util.Optional;

public interface FrEnedisPermissionRequest extends TimeframedPermissionRequest {
    FrEnedisPermissionRequest withStateBuilderFactory(StateBuilderFactory factory);

    PermissionProcessStatus status();

    Optional<String> usagePointId();

    @InvokeExtensions
    void setUsagePointId(String usagePointId);

    Granularity granularity();
}