package energy.eddie.regionconnector.fr.enedis.utils;

import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;

public class EnedisDuration {
    private final TimeframedPermissionRequest permissionRequest;

    public EnedisDuration(TimeframedPermissionRequest permissionRequest) {
        this.permissionRequest = permissionRequest;
    }

    @Override
    public String toString() {
        LocalDate endDate = permissionRequest.end();
        LocalDate now = LocalDate.now(ZONE_ID_FR);
        long days = ChronoUnit.DAYS.between(now, endDate);
        if (days <= 0) {
            days = 1; // minimum duration is 1 day
        }
        return "P%sD".formatted(days);
    }
}
