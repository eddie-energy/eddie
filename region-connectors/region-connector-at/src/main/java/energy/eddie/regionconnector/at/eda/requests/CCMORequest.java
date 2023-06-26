package energy.eddie.regionconnector.at.eda.requests;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.CMRequest;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.MarketParticipantDirectory;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.MeteringIntervallType;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.TransmissionCycle;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.AddressType;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.DocumentMode;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingAddress;
import energy.eddie.regionconnector.at.eda.EdaSchemaVersion;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedMeteringIntervalType;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedTransmissionCycle;
import energy.eddie.regionconnector.at.eda.utils.CMRequestId;
import energy.eddie.regionconnector.at.eda.xml.builders.customerconsent.cmrequest._01p10.CMRequestBuilder;
import energy.eddie.regionconnector.at.eda.xml.builders.customerconsent.cmrequest._01p10.MarketParticipantDirectoryBuilder;
import energy.eddie.regionconnector.at.eda.xml.builders.customerconsent.cmrequest._01p10.ProcessDirectoryBuilder;
import energy.eddie.regionconnector.at.eda.xml.builders.customerconsent.cmrequest._01p10.ReqTypeBuilder;
import energy.eddie.regionconnector.at.eda.xml.builders.customerprocesses.common.types._01p20.RoutingAddressBuilder;
import energy.eddie.regionconnector.at.eda.xml.builders.customerprocesses.common.types._01p20.RoutingHeaderBuilder;
import energy.eddie.regionconnector.at.eda.xml.builders.helper.Sector;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static java.util.Objects.requireNonNull;

public class CCMORequest {

    private static final MarketParticipantDirectoryBuilder MARKET_PARTICIPANT_DIRECTORY_BUILDER
            = new MarketParticipantDirectoryBuilder()
            .withMessageCode("ANFORDERUNG_CCMO")
            .withSector(Sector.ELECTRICITY)
            .withDocumentMode(DocumentMode.PROD)
            .withDuplicate(false)
            .withSchemaVersion(EdaSchemaVersion.CM_REQUEST_01_10.value());
    private final DsoIdAndMeteringPoint dsoIdAndMeteringPoint;
    private final CCMOTimeFrame timeframe;
    private final RequestDataType requestDataType;
    private final MeteringIntervallType meteringIntervalType;
    private final TransmissionCycle transmissionCycle;
    private final AtConfiguration configuration;

    public CCMORequest(DsoIdAndMeteringPoint dsoIdAndMeteringPoint,
                       CCMOTimeFrame timeframe,
                       AtConfiguration atConfiguration,
                       RequestDataType requestDataType,
                       AllowedMeteringIntervalType meteringIntervalType,
                       AllowedTransmissionCycle transmissionCycle) {
        requireNonNull(dsoIdAndMeteringPoint);
        requireNonNull(timeframe);
        requireNonNull(atConfiguration);
        requireNonNull(requestDataType);
        requireNonNull(meteringIntervalType);
        requireNonNull(transmissionCycle);

        this.dsoIdAndMeteringPoint = dsoIdAndMeteringPoint;
        this.timeframe = timeframe;
        this.configuration = atConfiguration;
        this.requestDataType = requestDataType;
        this.meteringIntervalType = meteringIntervalType.value();
        this.transmissionCycle = transmissionCycle.value();
    }

    private static RoutingAddress toRoutingAddress(String address) {
        requireNonNull(address);
        if (address.isBlank()) {
            throw new IllegalArgumentException("Address must not be null");
        }
        return new RoutingAddressBuilder()
                .withAddressType(AddressType.EC_NUMBER)
                .withMessageAddress(address)
                .build();
    }

    public CMRequest toCMRequest() throws InvalidDsoIdException {
        var marketParticipant = makeMarketParticipantDirectory();
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var processDirectory = new ProcessDirectoryBuilder();
        var messageId = new MessageId(toRoutingAddress(configuration.eligiblePartyId()), now);
        var requestId = new CMRequestId(messageId.toString());


        var requestType = new ReqTypeBuilder()
                .withDateFrom(timeframe.start())
                .withReqDatType(requestDataType.toString(timeframe))
                .withMeteringIntervall(this.meteringIntervalType)
                .withTransmissionCycle(this.transmissionCycle);
        timeframe.end().ifPresent(end -> requestType.withDateTo(end));
        processDirectory
                .withCMRequest(requestType.build())
                .withCMRequestId(requestId.toString())
                .withMessageId(messageId.toString())
                .withConversationId(messageId.toString())
                .withProcessDate(ZonedDateTime.now(configuration.timeZone()).toLocalDate());
        dsoIdAndMeteringPoint
                .meteringPoint()
                .ifPresent(processDirectory::withMeteringPoint);

        return new CMRequestBuilder()
                .withProcessDirectory(processDirectory.build())
                .withMarketParticipantDirectory(marketParticipant)
                .build();
    }

    private MarketParticipantDirectory makeMarketParticipantDirectory() throws InvalidDsoIdException {
        var routingHeader = new RoutingHeaderBuilder()
                .withSender(toRoutingAddress(configuration.eligiblePartyId()))
                .withReceiver(toRoutingAddress(dsoIdAndMeteringPoint.dsoId()))
                .withDocCreationDateTime(ZonedDateTime.now(configuration.timeZone()).toLocalDateTime())
                .build();

        return MARKET_PARTICIPANT_DIRECTORY_BUILDER
                .withRoutingHeader(routingHeader)
                .build();
    }
}
