package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.api.UsagePointType;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

public record SimpleFrEnedisPermissionRequest(
        String usagePointId,
        Granularity granularity,
        UsagePointType usagePointType,
        Optional<LocalDate> latestMeterReadingEndDate,
        String permissionId,
        String connectionId,
        String dataNeedId,
        PermissionProcessStatus status,
        DataSourceInformation dataSourceInformation,
        ZonedDateTime created,
        LocalDate start,
        LocalDate end
) implements FrEnedisPermissionRequest {
}
