package energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder;

public record SeriesPeriodBuilderFactory() {

    public SeriesPeriodBuilder create() {
        return new SeriesPeriodBuilder();
    }
}