package energy.eddie.api.agnostic;

import java.time.Duration;

/**
 * Contains all currently possible Granularities supported by at least one region connector. Granularities are valid ISO
 * 8601 durations.
 */
public enum Granularity {
    PT5M(Duration.ofMinutes(5)),
    PT10M(Duration.ofMinutes(10)),
    PT15M(Duration.ofMinutes(15)),
    PT30M(Duration.ofMinutes(30)),
    PT1H(Duration.ofHours(1)),
    P1D(Duration.ofDays(1)),
    P1M(Duration.ofDays(30)),
    P1Y(Duration.ofDays(365));

    private final Duration duration;

    Granularity(Duration duration) {
        this.duration = duration;
    }

    public static Granularity fromMinutes(int minutes) {
        for (var granularity : Granularity.values()) {
            if (granularity.minutes() == minutes) {
                return granularity;
            }
        }
        throw new IllegalArgumentException("Invalid granularity: " + minutes);
    }

    /**
     * Granularity roughly expressed in minutes. Used to compare if one granularity is greater than the other.
     */
    public int minutes() {
        return (int) duration.toMinutes();
    }

    public Duration duration() {
        return duration;
    }
}
