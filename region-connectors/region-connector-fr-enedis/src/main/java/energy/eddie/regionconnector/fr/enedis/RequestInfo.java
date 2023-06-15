package energy.eddie.regionconnector.fr.enedis;

import java.time.ZonedDateTime;

public record RequestInfo(String connectionId, ZonedDateTime start, ZonedDateTime end) {
}
