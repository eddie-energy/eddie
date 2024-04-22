package energy.eddie.regionconnector.fr.enedis.utils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;

public class EnedisDuration {
    private final LocalDate end;

    public EnedisDuration(LocalDate end) {
        this.end = end;
    }

    @Override
    public String toString() {
        LocalDate now = LocalDate.now(ZONE_ID_FR);
        long days = ChronoUnit.DAYS.between(now, end);
        if (days <= 0) {
            days = 1; // minimum duration is 1 day
        }
        return "P%sD".formatted(days);
    }
}
