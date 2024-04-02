package energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder;

import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.dto.EdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.Energy;
import energy.eddie.regionconnector.at.eda.dto.EnergyData;
import energy.eddie.regionconnector.shared.utils.EsmpDateTime;
import energy.eddie.regionconnector.shared.utils.EsmpTimeInterval;

import java.util.UUID;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static java.util.Objects.requireNonNull;

public class ValidatedHistoricalDataMarketDocumentBuilder {

    private final ValidatedHistoricalDataMarketDocument validatedHistoricalDataMarketDocument = new ValidatedHistoricalDataMarketDocument()
            .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
            .withMRID(UUID.randomUUID().toString())
            .withType(MessageTypeList.MEASUREMENT_VALUE_DOCUMENT)
            .withSenderMarketParticipantMarketRoleType(RoleTypeList.METERING_POINT_ADMINISTRATOR)
            .withReceiverMarketParticipantMarketRoleType(RoleTypeList.CONSUMER)
            .withProcessProcessType(ProcessTypeList.REALISED);
    private final SeriesPeriodBuilder seriesPeriodBuilder;
    private final TimeSeriesBuilder timeSeriesBuilder;

    public ValidatedHistoricalDataMarketDocumentBuilder(
            SeriesPeriodBuilder seriesPeriodBuilder,
            TimeSeriesBuilder timeSeriesBuilder
    ) {
        requireNonNull(seriesPeriodBuilder);
        requireNonNull(timeSeriesBuilder);

        this.seriesPeriodBuilder = seriesPeriodBuilder;
        this.timeSeriesBuilder = timeSeriesBuilder;
    }


    public ValidatedHistoricalDataMarketDocumentBuilder withRoutingHeaderData(
            EdaConsumptionRecord consumptionRecord,
            CodingSchemeTypeList receiverCodingScheme
    ) {
        EsmpDateTime esmpDateTime = new EsmpDateTime(consumptionRecord.documentCreationDateTime());
        validatedHistoricalDataMarketDocument
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
        var timeSeriesList = new ValidatedHistoricalDataMarketDocument.TimeSeriesList();
        for (Energy energy : consumptionRecord.energy()) {
            for (EnergyData energyData : energy.energyData()) {
                SeriesPeriodComplexType seriesPeriod = seriesPeriodBuilder
                        .withEnergy(energy)
                        .withEnergyData(energyData)
                        .build();
                TimeSeriesComplexType timeSeries = timeSeriesBuilder
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

    public ValidatedHistoricalDataMarketDocument build() {
        return validatedHistoricalDataMarketDocument;
    }
}
