package energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder;

import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;

public record ValidatedHistoricalDataMarketDocumentBuilderFactory(
        EnerginetConfiguration energinetConfiguration,
        CommonInformationModelConfiguration commonInformationModelConfiguration,
        TimeSeriesBuilderFactory timeSeriesBuilderFactory
) {

    public ValidatedHistoricalDataMarketDocumentBuilder create() {
        return new ValidatedHistoricalDataMarketDocumentBuilder(
                energinetConfiguration.customerId(),
                commonInformationModelConfiguration.eligiblePartyNationalCodingScheme(),
                timeSeriesBuilderFactory
        );
    }
}