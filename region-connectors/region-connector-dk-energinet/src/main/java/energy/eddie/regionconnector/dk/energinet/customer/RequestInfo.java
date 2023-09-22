package energy.eddie.regionconnector.dk.energinet.customer;

import java.time.ZonedDateTime;

public record RequestInfo(String connectionId, String refreshToken, String meteringPoint, String aggregation, ZonedDateTime start, ZonedDateTime end) {
}
