package energy.eddie.api.agnostic.data.needs;

/**
 * The result for accounting point data needs
 *
 * @param permissionTimeframe the start and end date from which data can be requested.
 */
public record AccountingPointDataNeedResult(Timeframe permissionTimeframe) implements DataNeedCalculationResult {
}
