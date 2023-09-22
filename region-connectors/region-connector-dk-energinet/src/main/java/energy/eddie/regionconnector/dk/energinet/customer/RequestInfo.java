package energy.eddie.regionconnector.dk.energinet.customer;

import java.time.ZonedDateTime;

public record RequestInfo(String connectionId, String refreshToken, String meteringPoint, ZonedDateTime start, ZonedDateTime end) {
}
