package energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder;

import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingHeader;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ConsumptionRecord;
import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.processing.utils.XmlGregorianCalenderUtils;
import energy.eddie.regionconnector.shared.utils.EsmpDateTime;

import java.util.UUID;

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

    public ValidatedHistoricalDataMarketDocumentBuilder(SeriesPeriodBuilder seriesPeriodBuilder, TimeSeriesBuilder timeSeriesBuilder) {
        requireNonNull(seriesPeriodBuilder);
        requireNonNull(timeSeriesBuilder);

        this.seriesPeriodBuilder = seriesPeriodBuilder;
        this.timeSeriesBuilder = timeSeriesBuilder;
    }


    public ValidatedHistoricalDataMarketDocumentBuilder withRoutingHeaderData(RoutingHeader routingHeader, CodingSchemeTypeList receiverCodingScheme) {
        EsmpDateTime esmpDateTime = new EsmpDateTime(XmlGregorianCalenderUtils.toUtcZonedDateTime(routingHeader.getDocumentCreationDateTime()));
        validatedHistoricalDataMarketDocument
                .withCreatedDateTime(esmpDateTime.toString())
                .withSenderMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                                .withValue(routingHeader.getSender().getMessageAddress())
                )
                .withReceiverMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(receiverCodingScheme)
                                .withValue(routingHeader.getReceiver().getMessageAddress())
                );
        return this;
    }

    public ValidatedHistoricalDataMarketDocumentBuilder withConsumptionRecord(ConsumptionRecord consumptionRecord) throws InvalidMappingException {
        var energy = consumptionRecord.getProcessDirectory().getEnergy().stream().findFirst().orElseThrow(() -> new InvalidMappingException("No Energy found in ProcessDirectory of ConsumptionRecord"));
        var energyData = energy.getEnergyData().stream().findFirst().orElseThrow(() -> new InvalidMappingException("No EnergyData found in Energy of ConsumptionRecord"));

        SeriesPeriodComplexType seriesPeriod = seriesPeriodBuilder
                .withEnergy(energy)
                .withEnergyData(energyData)
                .build();

        TimeSeriesComplexType timeSeries = timeSeriesBuilder
                .withMarketParticipantDirectory(consumptionRecord.getMarketParticipantDirectory())
                .withProcessDirectory(consumptionRecord.getProcessDirectory())
                .withEnergy(energy)
                .withEnergyData(energyData)
                .withSeriesPeriod(seriesPeriod)
                .build();

        var periodStart = new EsmpDateTime(XmlGregorianCalenderUtils.toUtcZonedDateTime(energy.getMeteringPeriodStart()));
        var periodEnd = new EsmpDateTime(XmlGregorianCalenderUtils.toUtcZonedDateTime(energy.getMeteringPeriodEnd()));

        validatedHistoricalDataMarketDocument
                .withTimeSeriesList(
                        new ValidatedHistoricalDataMarketDocument.TimeSeriesList()
                                .withTimeSeries(timeSeries)
                )
                .withPeriodTimeInterval(
                        new ESMPDateTimeIntervalComplexType()
                                .withStart(periodStart.toString())
                                .withEnd(periodEnd.toString())
                );

        return this;
    }

    public ValidatedHistoricalDataMarketDocument build() {
        return validatedHistoricalDataMarketDocument;
    }
}