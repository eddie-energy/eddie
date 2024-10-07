package energy.eddie.regionconnector.us.green.button.permission.request.api;

import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;
import energy.eddie.regionconnector.us.green.button.permission.events.MeterReading;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UsGreenButtonPermissionRequest extends MeterReadingPermissionRequest {
    Optional<String> scope();

    Optional<String> jumpOffUrl();

    Optional<ZonedDateTime> latestMeterReadingEndDateTime();

    Set<String> allowedMeters();

    List<MeterReading> lastMeterReadings();

    String authorizationUid();
}
