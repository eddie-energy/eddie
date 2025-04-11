package energy.eddie.regionconnector.be.fluvius.client.model;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class Readings<R> {

    private final List<R> rawReadings;
    private final Function<R, OffsetDateTime> timestampExtractor;

    public Readings(List<R> rawReadings, Function<R, OffsetDateTime> timestampExtractor) {
        this.rawReadings = rawReadings != null ? rawReadings : List.of();
        this.timestampExtractor = timestampExtractor;
    }

    public Optional<ZonedDateTime> getLastReadingTimestamp() {
        if (rawReadings.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(timestampExtractor.apply(rawReadings.getLast()))
                       .map(OffsetDateTime::toZonedDateTime);
    }
}
