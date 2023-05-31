package energy.eddie.regionconnector.at.eda.utils;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public record TruncatedZonedDateTime(ZonedDateTime zonedDateTime) {
    @Override
    public ZonedDateTime zonedDateTime() {
        return zonedDateTime.truncatedTo(ChronoUnit.DAYS);
    }
}
