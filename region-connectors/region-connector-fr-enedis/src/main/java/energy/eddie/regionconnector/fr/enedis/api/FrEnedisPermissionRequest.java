package energy.eddie.regionconnector.fr.enedis.api;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;

import java.util.Optional;

public interface FrEnedisPermissionRequest extends MeterReadingPermissionRequest {
    Optional<String> usagePointId();

    Granularity granularity();
}
