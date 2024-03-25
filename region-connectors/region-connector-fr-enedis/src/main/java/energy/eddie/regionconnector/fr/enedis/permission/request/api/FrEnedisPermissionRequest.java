package energy.eddie.regionconnector.fr.enedis.permission.request.api;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;
import energy.eddie.api.agnostic.process.model.annotations.InvokeExtensions;
import energy.eddie.regionconnector.fr.enedis.permission.request.StateBuilderFactory;

import java.util.Optional;

public interface FrEnedisPermissionRequest extends MeterReadingPermissionRequest {
    FrEnedisPermissionRequest withStateBuilderFactory(StateBuilderFactory factory);

    Optional<String> usagePointId();

    @InvokeExtensions
    void setUsagePointId(String usagePointId);

    Granularity granularity();
}
