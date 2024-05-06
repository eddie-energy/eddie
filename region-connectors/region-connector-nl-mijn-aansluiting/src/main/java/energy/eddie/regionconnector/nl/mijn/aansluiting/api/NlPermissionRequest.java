package energy.eddie.regionconnector.nl.mijn.aansluiting.api;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequest;

import java.time.ZonedDateTime;
import java.util.Map;

public interface NlPermissionRequest extends PermissionRequest {

    String codeVerifier();

    Granularity granularity();

    Map<String, ZonedDateTime> lastMeterReadings();
}
