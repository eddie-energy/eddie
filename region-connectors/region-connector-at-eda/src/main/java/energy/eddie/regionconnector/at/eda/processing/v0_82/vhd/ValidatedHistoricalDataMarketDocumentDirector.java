// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.processing.v0_82.vhd;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataMarketDocumentComplexType;
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

    public ValidatedHistoricalDataMarketDocumentComplexType createValidatedHistoricalDataMarketDocument(
            EdaConsumptionRecord consumptionRecord
    ) throws InvalidMappingException {
        return validatedHistoricalDataMarketDocumentBuilderFactory.create()
                                                                  .withRoutingHeaderData(consumptionRecord,
                                                                                         commonInformationModelConfiguration.eligiblePartyNationalCodingScheme())
                                                                  .withConsumptionRecord(consumptionRecord)
                                                                  .build();
    }
}
