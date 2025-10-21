package energy.eddie.regionconnector.at.eda.ponton.messages.cmrevoke._01p10;

import de.ponton.xp.adapter.api.domainvalues.*;
import de.ponton.xp.adapter.api.messages.OutboundMessage;
import energy.eddie.regionconnector.at.eda.models.MessageCodes;
import energy.eddie.regionconnector.at.eda.ponton.messages.cmrevoke.CMRevokeOutboundMessageFactory;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;

import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;

@Component
public class CMRevoke01p10OutboundMessageFactory implements CMRevokeOutboundMessageFactory {
    public static final LocalDate ACTIVE_FROM = LocalDate.of(2026, 4, 13);
    private final Jaxb2Marshaller marshaller;

    public CMRevoke01p10OutboundMessageFactory(Jaxb2Marshaller marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public OutboundMessage createOutboundMessage(CCMORevoke ccmoRevoke) {
        var revoke = new CMRevoke01p10(ccmoRevoke).cmRevoke();
        var outputStream = new ByteArrayOutputStream();
        marshaller.marshal(revoke, new StreamResult(outputStream));
        var inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        var senderId = ccmoRevoke.eligiblePartyId();
        var receiverId = ccmoRevoke.permissionRequest()
                                   .dataSourceInformation()
                                   .meteredDataAdministratorId();

        var outboundMetaData = OutboundMetaData.newBuilder()
                                               .setSenderId(new SenderId(senderId))
                                               .setReceiverId(new ReceiverId(receiverId))
                                               .setMessageType(createMessageType())
                                               .build();
        return OutboundMessage.newBuilder()
                              .setInputStream(inputStream)
                              .setOutboundMetaData(outboundMetaData)
                              .build();
    }

    @Override
    public boolean isActive(LocalDate date) {
        return !date.isBefore(ACTIVE_FROM);
    }

    private static MessageType createMessageType() {
        return new MessageType.MessageTypeBuilder()
                .setSchemaSet(new SchemaSet(MessageCodes.Revoke.EligibleParty.SCHEMA))
                .setVersion(new MessageTypeVersion(MessageCodes.Revoke.EligibleParty.VERSION))
                .setName(new MessageTypeName(MessageCodes.Revoke.EligibleParty.REVOKE))
                .setMimeType(new MimeType("text/xml"))
                .build();
    }
}
