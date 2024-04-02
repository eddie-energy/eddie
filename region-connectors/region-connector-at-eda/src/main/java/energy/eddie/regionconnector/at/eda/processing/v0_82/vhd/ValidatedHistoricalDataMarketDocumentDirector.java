package energy.eddie.regionconnector.at.eda.processing.v0_82.vhd;

import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataMarketDocument;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.dto.EdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder.ValidatedHistoricalDataMarketDocumentBuilderFactory;

import static java.util.Objects.requireNonNull;

public class ValidatedHistoricalDataMarketDocumentDirector {
    private final CommonInformationModelConfiguration commonInformationModelConfiguration;
    private final ValidatedHistoricalDataMarketDocumentBuilderFactory validatedHistoricalDataMarketDocumentBuilderFactory;

    public ValidatedHistoricalDataMarketDocumentDirector(
            CommonInformationModelConfiguration commonInformationModelConfiguration,
            ValidatedHistoricalDataMarketDocumentBuilderFactory validatedHistoricalDataMarketDocumentBuilderFactory
    ) {
        requireNonNull(commonInformationModelConfiguration);
        requireNonNull(validatedHistoricalDataMarketDocumentBuilderFactory);

        this.commonInformationModelConfiguration = commonInformationModelConfiguration;
        this.validatedHistoricalDataMarketDocumentBuilderFactory = validatedHistoricalDataMarketDocumentBuilderFactory;
    }

    public ValidatedHistoricalDataMarketDocument createValidatedHistoricalDataMarketDocument(EdaConsumptionRecord consumptionRecord) throws InvalidMappingException {
        return validatedHistoricalDataMarketDocumentBuilderFactory.create()
                                                                  .withRoutingHeaderData(consumptionRecord,
                                                                                         commonInformationModelConfiguration.eligiblePartyNationalCodingScheme())
                                                                  .withConsumptionRecord(consumptionRecord)
                                                                  .build();
    }
}
