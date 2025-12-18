package energy.eddie.regionconnector.be.fluvius.client.model;

import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class Readings<R> {

    private final List<R> rawReadings;
    private final Function<R, ZonedDateTime> timestampExtractor;

    public Readings(@Nullable List<R> rawReadings, Function<R, ZonedDateTime> timestampExtractor) {
        this.rawReadings = rawReadings != null ? rawReadings : List.of();
        this.timestampExtractor = timestampExtractor;
    }

    public Optional<ZonedDateTime> getLastReadingTimestamp() {
        if (rawReadings.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(timestampExtractor.apply(rawReadings.getLast()));
    }
}
