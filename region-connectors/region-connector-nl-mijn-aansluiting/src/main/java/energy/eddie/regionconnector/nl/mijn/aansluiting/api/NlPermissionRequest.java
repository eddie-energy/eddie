package energy.eddie.regionconnector.nl.mijn.aansluiting.api;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.shared.services.CommonPermissionRequest;

import java.time.ZonedDateTime;
import java.util.Map;

public interface NlPermissionRequest extends CommonPermissionRequest {

    String codeVerifier();

    Granularity granularity();

    Map<String, ZonedDateTime> lastMeterReadings();
}
