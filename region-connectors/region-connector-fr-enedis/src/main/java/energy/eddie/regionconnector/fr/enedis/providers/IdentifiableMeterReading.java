package energy.eddie.regionconnector.fr.enedis.providers;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.dto.readings.MeterReading;
import energy.eddie.regionconnector.shared.utils.MeterReadingEndDate;

import java.time.LocalDate;

public record IdentifiableMeterReading(
        FrEnedisPermissionRequest permissionRequest,
        MeterReading meterReading,
        MeterReadingType meterReadingType

) implements MeterReadingEndDate, IdentifiablePayload<FrEnedisPermissionRequest, MeterReading> {
    @Override
    public LocalDate meterReadingEndDate() {
        return meterReading().end();
    }

    @Override
    public MeterReading payload() {
        return meterReading();
    }
}
