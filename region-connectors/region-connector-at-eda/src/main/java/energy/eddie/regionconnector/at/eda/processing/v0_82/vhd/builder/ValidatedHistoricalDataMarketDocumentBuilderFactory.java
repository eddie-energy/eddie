package energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder;

public class ValidatedHistoricalDataMarketDocumentBuilderFactory {
    public ValidatedHistoricalDataMarketDocumentBuilder create() {
        return new ValidatedHistoricalDataMarketDocumentBuilder(new SeriesPeriodBuilder(), new TimeSeriesBuilder());
    }
}