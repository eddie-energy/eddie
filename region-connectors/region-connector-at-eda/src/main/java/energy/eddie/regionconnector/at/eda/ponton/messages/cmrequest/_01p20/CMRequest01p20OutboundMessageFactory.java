package energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest._01p20;

import de.ponton.xp.adapter.api.domainvalues.*;
import de.ponton.xp.adapter.api.messages.OutboundMessage;
import energy.eddie.regionconnector.at.eda.models.MessageCodes;
import energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest.CMRequestOutboundMessageFactory;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;

import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;

@Component
@SuppressWarnings("DuplicatedCode")
public class CMRequest01p20OutboundMessageFactory implements CMRequestOutboundMessageFactory {
    /**
     * The active from date of the message. The message is active from this date.
     * <p>From <a href="https://www.ebutilities.at/schemas/161">ebutilities</a>
     */
    private static final LocalDate ACTIVE_FROM = LocalDate.of(2024, 4, 8);
    private static final MessageType MESSAGETYPE = new MessageType.MessageTypeBuilder()
            .setSchemaSet(new SchemaSet("CM_REQ_ONL_01.20"))
            .setVersion(new MessageTypeVersion("01.20"))
            .setName(new MessageTypeName(MessageCodes.Request.CODE))
            .setMimeType(new MimeType(
                    "text/xml"))
            .build();

    private final Jaxb2Marshaller marshaller;

    public CMRequest01p20OutboundMessageFactory(Jaxb2Marshaller marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public OutboundMessage createOutboundMessage(CCMORequest ccmoRequest) {
        var request = new CMRequest01p20(ccmoRequest).cmRequest();
        var outputStream = new ByteArrayOutputStream();
        var result = new StreamResult(outputStream);
        marshaller.marshal(request, result);
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        return OutboundMessage.newBuilder()
                              .setInputStream(inputStream)
                              .setOutboundMetaData(outboundMetaData(ccmoRequest))
                              .build();
    }

    public OutboundMetaData outboundMetaData(CCMORequest ccmoRequest) {
        return OutboundMetaData
                .newBuilder()
                .setSenderId(new SenderId(ccmoRequest.eligiblePartyId()))
                .setReceiverId(new ReceiverId(ccmoRequest.dsoId()))
                .setMessageType(MESSAGETYPE)
                .build();
    }

    @Override
    public boolean isActive(LocalDate date) {
        return !ACTIVE_FROM.isAfter(date);
    }
}
