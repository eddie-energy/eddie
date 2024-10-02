package energy.eddie.api.agnostic;

/**
 * Contains all currently possible Granularities supported by at least one region connector. Granularities are valid ISO
 * 8601 durations.
 */
public enum Granularity {
    PT5M(5),
    PT10M(10),
    PT15M(15),
    PT30M(30),
    PT1H(60),
    P1D(1440),
    P1M(43200),
    P1Y(518400);

    private final int minutes;

    Granularity(int minutes) {
        this.minutes = minutes;
    }

    /**
     * Granularity roughly expressed in minutes. Used to compare if one granularity is greater than the other.
     */
    public int minutes() {
        return minutes;
    }

    public static Granularity fromMinutes(int minutes) {
        for (var granularity : Granularity.values()) {
            if (granularity.minutes == minutes) {
                return granularity;
            }
        }
        throw new IllegalArgumentException("Invalid granularity: " + minutes);
    }
}
