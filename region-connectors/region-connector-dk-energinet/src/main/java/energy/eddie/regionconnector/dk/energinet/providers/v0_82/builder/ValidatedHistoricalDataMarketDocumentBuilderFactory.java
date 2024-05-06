package energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder;

import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;

public record ValidatedHistoricalDataMarketDocumentBuilderFactory(
        CommonInformationModelConfiguration commonInformationModelConfiguration,
        TimeSeriesBuilderFactory timeSeriesBuilderFactory
) {

    public ValidatedHistoricalDataMarketDocumentBuilder create() {
        return new ValidatedHistoricalDataMarketDocumentBuilder(
                commonInformationModelConfiguration.eligiblePartyFallbackId(),
                commonInformationModelConfiguration.eligiblePartyNationalCodingScheme(),
                timeSeriesBuilderFactory
        );
    }
}
