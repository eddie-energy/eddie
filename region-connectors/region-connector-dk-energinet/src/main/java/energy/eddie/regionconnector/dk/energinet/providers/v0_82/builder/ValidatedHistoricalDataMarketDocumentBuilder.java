package energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder;

import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocument;
import energy.eddie.regionconnector.dk.energinet.customer.model.PeriodtimeInterval;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ValidatedHistoricalDataMarketDocumentBuilder {

    /**
     * The Global Location Number (GLN) of the sender of the message. This value is from the <a
     * href="https://api.eloverblik.dk/customerapi/index.html">API documentation</a> (from the API description linked in
     * the description)
     */
    public static final String GLOBAL_LOCATION_NUMBER = "5790001330583";
    public static final PartyIDStringComplexType SENDER_MARKET_PARTICIPANT_MRID = new PartyIDStringComplexType()
            .withValue(GLOBAL_LOCATION_NUMBER)
            .withCodingScheme(CodingSchemeTypeList.GS1);
    private final TimeSeriesBuilderFactory timeSeriesBuilderFactory;

    private final ValidatedHistoricalDataMarketDocumentComplexType validatedHistoricalDataMarketDocument = new ValidatedHistoricalDataMarketDocumentComplexType()
            .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
            .withType(MessageTypeList.MEASUREMENT_VALUE_DOCUMENT)
            .withSenderMarketParticipantMarketRoleType(RoleTypeList.METERING_POINT_ADMINISTRATOR)
            .withReceiverMarketParticipantMarketRoleType(RoleTypeList.CONSUMER)
            .withProcessProcessType(ProcessTypeList.REALISED);

    public ValidatedHistoricalDataMarketDocumentBuilder(
            String eligiblePartyId,
            CodingSchemeTypeList eligiblePartyNationalCodingScheme,
            TimeSeriesBuilderFactory timeSeriesBuilderFactory
    ) {
        validatedHistoricalDataMarketDocument.withReceiverMarketParticipantMRID(
                new PartyIDStringComplexType()
                        .withValue(eligiblePartyId)
                        .withCodingScheme(eligiblePartyNationalCodingScheme)
        );
        this.timeSeriesBuilderFactory = timeSeriesBuilderFactory;
    }

    public ValidatedHistoricalDataMarketDocumentBuilder withMyEnergyDataMarketDocument(MyEnergyDataMarketDocument myEnergyDataMarketDocument) {
        var timeSeriesList = Objects.requireNonNull(myEnergyDataMarketDocument.getTimeSeries());
        validatedHistoricalDataMarketDocument
                .withMRID(Optional.ofNullable(myEnergyDataMarketDocument.getmRID())
                                  .orElse(UUID.randomUUID().toString()))
                .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
                .withType(MessageTypeList.MEASUREMENT_VALUE_DOCUMENT)
                .withCreatedDateTime(myEnergyDataMarketDocument.getCreatedDateTime())
                .withSenderMarketParticipantMarketRoleType(RoleTypeList.METERING_POINT_ADMINISTRATOR)
                .withReceiverMarketParticipantMarketRoleType(RoleTypeList.CONSUMER)
                .withProcessProcessType(ProcessTypeList.REALISED)
                .withSenderMarketParticipantMRID(SENDER_MARKET_PARTICIPANT_MRID)
                .withPeriodTimeInterval(fromPeriodTimeInterval(Objects.requireNonNull(myEnergyDataMarketDocument.getPeriodTimeInterval())))
                .withTimeSeriesList(timeSeriesBuilderFactory.create().withTimeSeriesList(timeSeriesList).build());
        return this;
    }

    private ESMPDateTimeIntervalComplexType fromPeriodTimeInterval(PeriodtimeInterval periodTimeInterval) {
        return new ESMPDateTimeIntervalComplexType()
                .withStart(periodTimeInterval.getStart())
                .withEnd(periodTimeInterval.getEnd());
    }

    public ValidatedHistoricalDataMarketDocumentComplexType build() {
        return validatedHistoricalDataMarketDocument;
    }
}
