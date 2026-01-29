// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder;

import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.dto.EdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.Energy;
import energy.eddie.regionconnector.at.eda.dto.EnergyData;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpDateTime;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static java.util.Objects.requireNonNull;

public class ValidatedHistoricalDataMarketDocumentBuilder {

    private final ValidatedHistoricalDataMarketDocumentComplexType validatedHistoricalDataMarketDocument = new ValidatedHistoricalDataMarketDocumentComplexType()
            .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
            .withType(MessageTypeList.MEASUREMENT_VALUE_DOCUMENT)
            .withSenderMarketParticipantMarketRoleType(RoleTypeList.METERING_POINT_ADMINISTRATOR)
            .withReceiverMarketParticipantMarketRoleType(RoleTypeList.CONSUMER)
            .withProcessProcessType(ProcessTypeList.REALISED);
    private final SeriesPeriodBuilderFactory seriesPeriodBuilderFactory;
    private final TimeSeriesBuilderFactory timeSeriesBuilderFactory;

    public ValidatedHistoricalDataMarketDocumentBuilder(
            SeriesPeriodBuilderFactory seriesPeriodBuilderFactory,
            TimeSeriesBuilderFactory timeSeriesBuilderFactory
    ) {
        requireNonNull(seriesPeriodBuilderFactory);
        requireNonNull(timeSeriesBuilderFactory);

        this.seriesPeriodBuilderFactory = seriesPeriodBuilderFactory;
        this.timeSeriesBuilderFactory = timeSeriesBuilderFactory;
    }


    public ValidatedHistoricalDataMarketDocumentBuilder withRoutingHeaderData(
            EdaConsumptionRecord consumptionRecord,
            CodingSchemeTypeList receiverCodingScheme
    ) {
        EsmpDateTime esmpDateTime = new EsmpDateTime(consumptionRecord.documentCreationDateTime());
        validatedHistoricalDataMarketDocument
                .withMRID(consumptionRecord.messageId())
                .withCreatedDateTime(esmpDateTime.toString())
                .withSenderMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                                .withValue(consumptionRecord.senderMessageAddress())
                )
                .withReceiverMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(receiverCodingScheme)
                                .withValue(consumptionRecord.receiverMessageAddress())
                );
        return this;
    }

    public ValidatedHistoricalDataMarketDocumentBuilder withConsumptionRecord(EdaConsumptionRecord consumptionRecord) throws InvalidMappingException {
        var timeSeriesList = new ValidatedHistoricalDataMarketDocumentComplexType.TimeSeriesList();
        for (Energy energy : consumptionRecord.energy()) {
            for (EnergyData energyData : energy.energyData()) {
                SeriesPeriodComplexType seriesPeriod = seriesPeriodBuilderFactory
                        .create()
                        .withEnergy(energy)
                        .withEnergyData(energyData)
                        .build();
                TimeSeriesComplexType timeSeries = timeSeriesBuilderFactory
                        .create()
                        .withConsumptionRecord(consumptionRecord)
                        .withEnergy(energy)
                        .withEnergyData(energyData)
                        .withSeriesPeriod(seriesPeriod)
                        .build();
                timeSeriesList.withTimeSeries(timeSeries);
            }
        }

        var interval = new EsmpTimeInterval(
                consumptionRecord.startDate().atStartOfDay(AT_ZONE_ID),
                consumptionRecord.endDate().atStartOfDay(AT_ZONE_ID)
        );

        validatedHistoricalDataMarketDocument
                .withTimeSeriesList(timeSeriesList)
                .withPeriodTimeInterval(
                        new ESMPDateTimeIntervalComplexType()
                                .withStart(interval.start())
                                .withEnd(interval.end())
                );

        return this;
    }

    public ValidatedHistoricalDataMarketDocumentComplexType build() {
        return validatedHistoricalDataMarketDocument;
    }
}
