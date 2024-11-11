package energy.eddie.regionconnector.fr.enedis.api;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;
import energy.eddie.regionconnector.shared.services.CommonPermissionRequest;

public interface FrEnedisPermissionRequest extends CommonPermissionRequest {
    String usagePointId();

    Granularity granularity();

    UsagePointType usagePointType();
}
