package energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder;

public record TimeSeriesBuilderFactory(SeriesPeriodBuilderFactory factory) {

    public TimeSeriesBuilder create() {
        return new TimeSeriesBuilder(factory);
    }
}