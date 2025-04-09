package energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest._01p21;

import at.ebutilities.schemata.customerconsent.cmrequest._01p21.*;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.AddressType;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.DocumentMode;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingAddress;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingHeader;
import energy.eddie.regionconnector.at.eda.models.MessageCodes;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.xml.helper.DateTimeConverter;
import energy.eddie.regionconnector.at.eda.xml.helper.Sector;
import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static java.util.Objects.requireNonNull;

@SuppressWarnings("DuplicatedCode")
public record CMRequest01p21(
        CCMORequest ccmoRequest
) {

    public static final String SCHEMA_VERSION = "01.21";

    public CMRequest cmRequest() {
        return new CMRequest()
                .withMarketParticipantDirectory(makeMarketParticipantDirectory(ccmoRequest))
                .withProcessDirectory(makeProcessDirectory(ccmoRequest));
    }

    private MarketParticipantDirectory makeMarketParticipantDirectory(CCMORequest ccmoRequest) {
        return new MarketParticipantDirectory()
                .withMessageCode(MessageCodes.Request.CODE)
                .withSector(Sector.ELECTRICITY.value())
                .withDocumentMode(DocumentMode.PROD)
                .withDuplicate(false)
                .withSchemaVersion(SCHEMA_VERSION)
                .withRoutingHeader(new RoutingHeader()
                                           .withSender(toRoutingAddress(ccmoRequest.eligiblePartyId()))
                                           .withReceiver(toRoutingAddress(ccmoRequest.dsoId()))
                                           .withDocumentCreationDateTime(
                                                   DateTimeConverter.dateTimeToXml(LocalDateTime.now(AT_ZONE_ID))
                                           )
                );
    }

    private ProcessDirectory makeProcessDirectory(CCMORequest ccmoRequest) {
        var messageId = ccmoRequest.messageId();
        String conversationID = ccmoRequest.messageId();
        return new ProcessDirectory()
                .withCMRequest(makeReqType(ccmoRequest))
                .withCMRequestId(ccmoRequest.cmRequestId())
                .withMessageId(messageId)
                .withConversationId(conversationID)
                .withProcessDate(DateTimeConverter.dateToXml(LocalDate.now(AT_ZONE_ID)))
                .withMeteringPoint(ccmoRequest.meteringPointId().orElse(null));
    }

    private static RoutingAddress toRoutingAddress(String address) {
        requireNonNull(address);
        if (address.isBlank()) {
            throw new IllegalArgumentException("Address must not be null");
        }
        return new RoutingAddress()
                .withAddressType(AddressType.EC_NUMBER)
                .withMessageAddress(address);
    }


    private ReqType makeReqType(CCMORequest ccmoRequest) {
        return new ReqType()
                .withReqDatType(ccmoRequest.requestDataType().toString(ccmoRequest.timeframe()))
                .withMeteringIntervall(meteringIntervall(ccmoRequest))
                .withTransmissionCycle(transmissionCycle(ccmoRequest))
                .withDateFrom(DateTimeConverter.dateTimeToXml(ccmoRequest.start().atStartOfDay(AT_ZONE_ID)))
                .withDateTo(ccmoRequest.end()
                                       .map(end -> end.atStartOfDay(AT_ZONE_ID))
                                       .map(DateTimeConverter::dateTimeToXml)
                                       .orElse(null)
                );
    }

    @Nullable
    private MeteringIntervallType meteringIntervall(CCMORequest ccmoRequest) {
        return switch (ccmoRequest.granularity()) {
            case PT15M -> MeteringIntervallType.QH;
            case P1D -> MeteringIntervallType.D;
            case null -> null;
        };
    }

    private TransmissionCycle transmissionCycle(CCMORequest ccmoRequest) {
        return switch (ccmoRequest.transmissionCycle()) {
            case D -> TransmissionCycle.D;
            case M -> TransmissionCycle.M;
        };
    }
}
