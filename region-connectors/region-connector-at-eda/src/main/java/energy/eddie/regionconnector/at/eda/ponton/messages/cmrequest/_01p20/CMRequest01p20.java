package energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest._01p20;

import at.ebutilities.schemata.customerconsent.cmrequest._01p20.*;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.AddressType;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.DocumentMode;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingAddress;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingHeader;
import energy.eddie.regionconnector.at.eda.EdaSchemaVersion;
import energy.eddie.regionconnector.at.eda.models.MessageCodes;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.xml.helper.DateTimeConverter;
import energy.eddie.regionconnector.at.eda.xml.helper.Sector;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static java.util.Objects.requireNonNull;

@SuppressWarnings("DuplicatedCode")
public record CMRequest01p20(
        CCMORequest ccmoRequest
) {
    public at.ebutilities.schemata.customerconsent.cmrequest._01p20.CMRequest cmRequest() {
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
                .withSchemaVersion(EdaSchemaVersion.CM_REQUEST_01_10.value())
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
        String prefixedConversationId = ccmoRequest.conversationIdPrefix()
                                                   .map(prefix -> prefix + messageId)
                                                   .orElse(messageId);
        return new ProcessDirectory()
                .withCMRequest(makeReqType(ccmoRequest))
                .withCMRequestId(ccmoRequest.cmRequestId())
                .withMessageId(messageId)
                .withConversationId(prefixedConversationId)
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

    private MeteringIntervallType meteringIntervall(CCMORequest ccmoRequest) {
        return switch (ccmoRequest.intervalType()) {
            case QH -> MeteringIntervallType.QH;
            case D -> MeteringIntervallType.D;
        };
    }

    private TransmissionCycle transmissionCycle(CCMORequest ccmoRequest) {
        return switch (ccmoRequest.transmissionCycle()) {
            case D -> TransmissionCycle.D;
            case M -> TransmissionCycle.M;
        };
    }
}
