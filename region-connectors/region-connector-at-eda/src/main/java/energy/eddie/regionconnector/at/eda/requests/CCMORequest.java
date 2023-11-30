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
import energy.eddie.regionconnector.at.eda.utils.DateTimeConstants;
import energy.eddie.regionconnector.at.eda.xml.builders.customerconsent.cmrequest._01p10.CMRequestBuilder;
import energy.eddie.regionconnector.at.eda.xml.builders.customerconsent.cmrequest._01p10.MarketParticipantDirectoryBuilder;
import energy.eddie.regionconnector.at.eda.xml.builders.customerconsent.cmrequest._01p10.ProcessDirectoryBuilder;
import energy.eddie.regionconnector.at.eda.xml.builders.customerconsent.cmrequest._01p10.ReqTypeBuilder;
import energy.eddie.regionconnector.at.eda.xml.builders.customerprocesses.common.types._01p20.RoutingAddressBuilder;
import energy.eddie.regionconnector.at.eda.xml.builders.customerprocesses.common.types._01p20.RoutingHeaderBuilder;
import energy.eddie.regionconnector.at.eda.xml.builders.helper.Sector;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class CCMORequest {
    public static final int DSO_ID_LENGTH = 8;

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
    private final ZonedDateTime timestamp;

    public CCMORequest(DsoIdAndMeteringPoint dsoIdAndMeteringPoint,
                       CCMOTimeFrame timeframe,
                       AtConfiguration atConfiguration,
                       RequestDataType requestDataType,
                       AllowedMeteringIntervalType meteringIntervalType,
                       AllowedTransmissionCycle transmissionCycle,
                       ZonedDateTime timestamp) {
        requireNonNull(dsoIdAndMeteringPoint);
        requireNonNull(timeframe);
        requireNonNull(atConfiguration);
        requireNonNull(requestDataType);
        requireNonNull(meteringIntervalType);
        requireNonNull(transmissionCycle);
        requireNonNull(timestamp);

        this.dsoIdAndMeteringPoint = dsoIdAndMeteringPoint;
        this.timeframe = timeframe;
        this.configuration = atConfiguration;
        this.requestDataType = requestDataType;
        this.meteringIntervalType = meteringIntervalType.value();
        this.transmissionCycle = transmissionCycle.value();
        this.timestamp = timestamp;
    }

    public CCMORequest(DsoIdAndMeteringPoint dsoIdAndMeteringPoint,
                       CCMOTimeFrame timeframe,
                       AtConfiguration atConfiguration,
                       RequestDataType requestDataType,
                       AllowedMeteringIntervalType meteringIntervalType,
                       AllowedTransmissionCycle transmissionCycle) {
        this(dsoIdAndMeteringPoint, timeframe, atConfiguration,
                requestDataType, meteringIntervalType, transmissionCycle, ZonedDateTime.now(ZoneOffset.UTC));
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

    /**
     * The messageId also known as conversation id.
     *
     * @return the messageId, also known as conversation id.
     */
    public String messageId() {
        return new MessageId(toRoutingAddress(configuration.eligiblePartyId()), timestamp).toString();
    }

    public String cmRequestId() {
        return new CMRequestId(messageId()).toString();
    }

    public Optional<String> meteringPointId() {
        return dsoIdAndMeteringPoint.meteringPoint();
    }

    public String dsoId() {
        return dsoIdAndMeteringPoint.dsoId();
    }

    public LocalDate dataFrom() {
        return timeframe.start();
    }

    public Optional<LocalDate> dataTo() {
        return timeframe.end();
    }

    public CMRequest toCMRequest() throws InvalidDsoIdException {
        var marketParticipant = makeMarketParticipantDirectory();
        var processDirectory = new ProcessDirectoryBuilder();
        var messageId = messageId();

        var requestType = new ReqTypeBuilder()
                .withDateFrom(timeframe.start())
                .withReqDatType(requestDataType.toString(timeframe))
                .withMeteringIntervall(this.meteringIntervalType)
                .withTransmissionCycle(this.transmissionCycle);
        timeframe.end().ifPresent(requestType::withDateTo);
        processDirectory
                .withCMRequest(requestType.build())
                .withCMRequestId(cmRequestId())
                .withMessageId(messageId)
                .withConversationId(messageId)
                .withProcessDate(LocalDate.now(DateTimeConstants.AT_ZONE_ID));

        var optionalMeteringPoint = dsoIdAndMeteringPoint.meteringPoint();
        if (optionalMeteringPoint.isPresent()) {
            String meteringPoint = optionalMeteringPoint.get();
            if (!Objects.equals(dsoIdAndMeteringPoint.dsoId(), meteringPoint.substring(0, DSO_ID_LENGTH))) {
                throw new InvalidDsoIdException("The dsoId does not match the dsoId of the metering point");
            }

            processDirectory.withMeteringPoint(meteringPoint);
        }

        return new CMRequestBuilder()
                .withProcessDirectory(processDirectory.build())
                .withMarketParticipantDirectory(marketParticipant)
                .build();
    }

    private MarketParticipantDirectory makeMarketParticipantDirectory() {
        var routingHeader = new RoutingHeaderBuilder()
                .withSender(toRoutingAddress(configuration.eligiblePartyId()))
                .withReceiver(toRoutingAddress(dsoIdAndMeteringPoint.dsoId()))
                .withDocCreationDateTime(LocalDateTime.now(DateTimeConstants.AT_ZONE_ID))
                .build();

        return MARKET_PARTICIPANT_DIRECTORY_BUILDER
                .withRoutingHeader(routingHeader)
                .build();
    }
}