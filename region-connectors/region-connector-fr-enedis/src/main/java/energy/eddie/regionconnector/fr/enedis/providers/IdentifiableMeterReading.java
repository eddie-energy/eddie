package energy.eddie.regionconnector.fr.enedis.providers;

import energy.eddie.regionconnector.fr.enedis.dto.MeterReading;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.shared.utils.MeterReadingEndDate;

import java.time.LocalDate;

public record IdentifiableMeterReading(
        FrEnedisPermissionRequest permissionRequest,
        MeterReading meterReading
) implements MeterReadingEndDate {
    @Override
    public LocalDate meterReadingEndDate() {
        return meterReading().end();
    }
}
