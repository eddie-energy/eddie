package energy.eddie.regionconnector.fr.enedis.providers;

import energy.eddie.regionconnector.fr.enedis.dto.MeterReading;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;

public record IdentifiableMeterReading(
        FrEnedisPermissionRequest permissionRequest,
        MeterReading meterReading
) {
}