package energy.eddie.api.agnostic.data.needs;

import energy.eddie.api.agnostic.Granularity;

import java.util.List;

/**
 * Result type for the calculation of validated historical data data-needs.
 *
 * @param granularities       The calculated granularities, which is the intersection of the needed granularities by the
 *                            data-need and the supported granularities of the region-connector.
 * @param permissionTimeframe The timeframe of the permission request. States a start date from which data can be
 *                            requested and an end date until data can be requested.
 * @param energyTimeframe     The start and end date of the requested data.
 */
public record ValidatedHistoricalDataDataNeedResult(
        List<Granularity> granularities,
        Timeframe permissionTimeframe,
        Timeframe energyTimeframe
) implements DataNeedCalculationResult {
}
