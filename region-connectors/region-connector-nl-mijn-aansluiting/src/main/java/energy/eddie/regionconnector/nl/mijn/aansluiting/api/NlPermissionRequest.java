package energy.eddie.regionconnector.nl.mijn.aansluiting.api;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;

import java.time.ZonedDateTime;
import java.util.Map;

public interface NlPermissionRequest extends MeterReadingPermissionRequest {

    String codeVerifier();

    Granularity granularity();

    Map<String, ZonedDateTime> lastMeterReadings();
}
