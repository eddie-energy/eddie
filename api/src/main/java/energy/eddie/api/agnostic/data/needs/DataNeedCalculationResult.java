package energy.eddie.api.agnostic.data.needs;

public sealed interface DataNeedCalculationResult permits AccountingPointDataNeedResult, DataNeedNotFoundResult, DataNeedNotSupportedResult, ValidatedHistoricalDataDataNeedResult {
}
