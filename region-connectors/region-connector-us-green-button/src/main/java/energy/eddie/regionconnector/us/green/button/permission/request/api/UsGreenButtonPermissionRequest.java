package energy.eddie.regionconnector.us.green.button.permission.request.api;

import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;

import java.time.ZonedDateTime;
import java.util.Optional;

public interface UsGreenButtonPermissionRequest extends MeterReadingPermissionRequest {
    Optional<String> scope();

    Optional<String> jumpOffUrl();

    Optional<ZonedDateTime> latestMeterReadingEndDateTime();
}
