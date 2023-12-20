package energy.eddie.api.agnostic;

/**
 * Contains all currently possible Granularities supported by at least one {@link energy.eddie.api.v0.RegionConnector}.
 * Granularities are valid ISO 8601 durations.
 */
public enum Granularity {
    PT5M,
    PT10M,
    PT15M,
    PT30M,
    PT1H,
    P1D,
    P1M,
    P1Y;

    @Override
    public String toString() {
        return this.name();
    }
}