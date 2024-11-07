package energy.eddie.regionconnector.shared.services;

import energy.eddie.api.agnostic.Granularity;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

public interface CommonPermissionRequest {

    String dataNeedId();
    String permissionId();
    Granularity granularity();
    LocalDate start();
    LocalDate end();
    String customerIdentification();
    String meteringPointEAN();
    Optional<ZonedDateTime> latestMeterReading();
}
