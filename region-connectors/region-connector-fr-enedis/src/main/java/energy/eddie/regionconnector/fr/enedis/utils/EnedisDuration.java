package energy.eddie.regionconnector.fr.enedis.utils;

import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class EnedisDuration {
    private final TimeframedPermissionRequest permissionRequest;

    public EnedisDuration(TimeframedPermissionRequest permissionRequest) {
        this.permissionRequest = permissionRequest;
    }

    @Override
    public String toString() {
        ZonedDateTime endDate = Objects.requireNonNull(permissionRequest.end());
        LocalDate now = LocalDate.now(endDate.getZone());
        long days = ChronoUnit.DAYS.between(now, endDate.toLocalDate());
        if (days <= 0) {
            days = 1; // minimum duration is 1 day
        }
        return "P%sD".formatted(days);
    }
}