package energy.eddie.regionconnector.fr.enedis.utils;

import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class EnedisDuration {
    private final TimeframedPermissionRequest permissionRequest;

    public EnedisDuration(TimeframedPermissionRequest permissionRequest) {
        this.permissionRequest = permissionRequest;
    }

    @Override
    public String toString() {
        ZonedDateTime endDate = permissionRequest.end();
        ZonedDateTime now = ZonedDateTime.now(endDate.getZone());
        long days = ChronoUnit.DAYS.between(now, endDate);
        if (days <= 0) {
            days = 1;
        }
        return "P%sD".formatted(days);
    }
}
