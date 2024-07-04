package energy.eddie.regionconnector.fr.enedis.api;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;

public interface FrEnedisPermissionRequest extends MeterReadingPermissionRequest {
    String usagePointId();

    Granularity granularity();
}
