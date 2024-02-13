package energy.eddie.regionconnector.at.eda.requests;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.*;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.DocumentMode;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingHeader;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.at.eda.EdaSchemaVersion;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.models.MessageCodes;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedTransmissionCycle;
import energy.eddie.regionconnector.at.eda.utils.CMRequestId;
import energy.eddie.regionconnector.at.eda.utils.DateTimeConstants;
import energy.eddie.regionconnector.at.eda.xml.helper.DateTimeConverter;
import energy.eddie.regionconnector.at.eda.xml.helper.Sector;
import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class CCMORequest {
    public static final int DSO_ID_LENGTH = 8;

    private final DsoIdAndMeteringPoint dsoIdAndMeteringPoint;
    private final CCMOTimeFrame timeframe;
    private final RequestDataType requestDataType;
    private final Granularity granularity;
    private final TransmissionCycle transmissionCycle;
    private final AtConfiguration configuration;
    private final ZonedDateTime timestamp;

    public CCMORequest(DsoIdAndMeteringPoint dsoIdAndMeteringPoint,
                       CCMOTimeFrame timeframe,
                       AtConfiguration atConfiguration,
                       RequestDataType requestDataType,
                       Granularity granularity,
                       AllowedTransmissionCycle transmissionCycle,
                       ZonedDateTime timestamp) {
        requireNonNull(dsoIdAndMeteringPoint);
        requireNonNull(timeframe);
        requireNonNull(atConfiguration);
        requireNonNull(requestDataType);
        requireNonNull(granularity);
        requireNonNull(transmissionCycle);
        requireNonNull(timestamp);

        this.dsoIdAndMeteringPoint = dsoIdAndMeteringPoint;
        this.timeframe = timeframe;
        this.configuration = atConfiguration;
        this.requestDataType = requestDataType;
        this.granularity = granularity;
        this.transmissionCycle = transmissionCycle.value();
        this.timestamp = timestamp;
    }

    public CCMORequest(DsoIdAndMeteringPoint dsoIdAndMeteringPoint,
                       CCMOTimeFrame timeframe,
                       AtConfiguration atConfiguration,
                       RequestDataType requestDataType,
                       Granularity granularity,
                       AllowedTransmissionCycle transmissionCycle) {
        this(dsoIdAndMeteringPoint, timeframe, atConfiguration,
                requestDataType, granularity, transmissionCycle, ZonedDateTime.now(ZoneOffset.UTC));
    }

    /**
     * The messageId also known as conversation id.
     *
     * @return the messageId, also known as conversation id.
     */
    public String messageId() {
        return new MessageId(new CCMOAddress(configuration.eligiblePartyId()).toRoutingAddress(), timestamp).toString();
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

    public ZonedDateTime start() {
        return timeframe.start();
    }

    public Optional<ZonedDateTime> end() {
        return timeframe.end();
    }

    public CMRequest toCMRequest() throws InvalidDsoIdException {
        return new CMRequest()
                .withMarketParticipantDirectory(makeMarketParticipantDirectory())
                .withProcessDirectory(makeProcessDirectory());
    }

    private MarketParticipantDirectory makeMarketParticipantDirectory() {
        return new MarketParticipantDirectory()
                .withMessageCode(MessageCodes.Request.CODE)
                .withSector(Sector.ELECTRICITY.value())
                .withDocumentMode(DocumentMode.PROD)
                .withDuplicate(false)
                .withSchemaVersion(EdaSchemaVersion.CM_REQUEST_01_10.value())
                .withRoutingHeader(new RoutingHeader()
                        .withSender(new CCMOAddress(configuration.eligiblePartyId()).toRoutingAddress())
                        .withReceiver(new CCMOAddress(dsoIdAndMeteringPoint.dsoId()).toRoutingAddress())
                        .withDocumentCreationDateTime(
                                DateTimeConverter.dateTimeToXml(LocalDateTime.now(DateTimeConstants.AT_ZONE_ID))
                        )
                );
    }

    private ProcessDirectory makeProcessDirectory() throws InvalidDsoIdException {
        var messageId = messageId();
        String prefixedConversationId = configuration.conversationIdPrefix()
                .map(prefix -> prefix + messageId)
                .orElse(messageId);
        return new ProcessDirectory()
                .withCMRequest(makeReqType())
                .withCMRequestId(cmRequestId())
                .withMessageId(messageId)
                .withConversationId(prefixedConversationId)
                .withProcessDate(DateTimeConverter.dateToXml(LocalDate.now(DateTimeConstants.AT_ZONE_ID)))
                .withMeteringPoint(meteringPointOrThrow());
    }

    @Nullable
    private String meteringPointOrThrow() throws InvalidDsoIdException {
        var meteringPointOptional = dsoIdAndMeteringPoint.meteringPoint();

        if (meteringPointOptional.isEmpty()) {
            return null;
        }

        return dsoIdAndMeteringPoint.meteringPoint()
                .filter(meteringPoint ->
                        Objects.equals(dsoIdAndMeteringPoint.dsoId(), meteringPoint.substring(0, DSO_ID_LENGTH)))
                .orElseThrow(() -> new InvalidDsoIdException("The dsoId does not match the dsoId of the metering point"));
    }

    private ReqType makeReqType() {
        return new ReqType()
                .withReqDatType(requestDataType.toString(timeframe))
                .withMeteringIntervall(meteringIntervall())
                .withTransmissionCycle(this.transmissionCycle)
                .withDateFrom(DateTimeConverter.dateTimeToXml(timeframe.start()))
                .withDateTo(timeframe.end()
                        .map(DateTimeConverter::dateTimeToXml)
                        .orElse(null)
                );
    }

    private MeteringIntervallType meteringIntervall() {
        return switch (granularity) {
            case PT15M -> MeteringIntervallType.QH;
            case PT1H -> MeteringIntervallType.H;
            case P1D -> MeteringIntervallType.D;
            default -> throw new IllegalArgumentException("Granularity not supported: " + granularity);
        };
    }
}