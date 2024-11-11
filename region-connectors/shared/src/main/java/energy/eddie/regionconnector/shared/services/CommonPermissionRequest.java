package energy.eddie.regionconnector.shared.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;

import java.time.ZonedDateTime;
import java.util.Optional;

public interface CommonPermissionRequest extends MeterReadingPermissionRequest {

    Granularity granularity();
    String customerIdentification();
    String meteringPointEAN();
    Optional<ZonedDateTime> latestMeterReading();
    String usagePointId();
}
