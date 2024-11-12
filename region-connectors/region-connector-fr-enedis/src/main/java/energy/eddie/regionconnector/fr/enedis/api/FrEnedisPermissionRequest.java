package energy.eddie.regionconnector.fr.enedis.api;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.shared.services.CommonPermissionRequest;

public interface FrEnedisPermissionRequest extends CommonPermissionRequest {
    @Override
    String usagePointId();
    @Override
    Granularity granularity();

    UsagePointType usagePointType();
}
