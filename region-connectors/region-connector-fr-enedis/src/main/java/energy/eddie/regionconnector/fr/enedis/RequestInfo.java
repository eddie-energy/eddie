package energy.eddie.regionconnector.fr.enedis;

import java.time.ZonedDateTime;

public record RequestInfo(String connectionId, String dataNeedId, ZonedDateTime start, ZonedDateTime end) {
}
