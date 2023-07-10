package energy.eddie.regionconnector.at.eda.requests;

import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingAddress;

import java.time.ZonedDateTime;

import static java.util.Objects.requireNonNull;

public record MessageId(RoutingAddress address, ZonedDateTime dateTime) {
    private static final String FORMAT = "%sT%s";

    public MessageId {
        requireNonNull(address);
        requireNonNull(dateTime);
    }

    @Override
    public String toString() {
        return FORMAT.formatted(address.getMessageAddress(), dateTime.toInstant().toEpochMilli());
    }
}
