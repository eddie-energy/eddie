package energy.eddie.regionconnector.at.eda.ponton.messages.cprequest._1p12;

import de.ponton.xp.adapter.api.domainvalues.*;
import de.ponton.xp.adapter.api.messages.OutboundMessage;
import energy.eddie.regionconnector.at.eda.models.MessageCodes;
import energy.eddie.regionconnector.at.eda.ponton.messages.cprequest.CPRequestOutboundMessageFactory;
import energy.eddie.regionconnector.at.eda.requests.CPRequestCR;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;

import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;


@Component
@SuppressWarnings("DuplicatedCode")
public class CPRequestOutbound01p12MessageFactory implements CPRequestOutboundMessageFactory {

    /**
     * The active from date of the message. The message is active from this date.
     * <p>From <a href="https://www.ebutilities.at/schemas/68">ebutilities</a>
     */
    private static final LocalDate ACTIVE_FROM = LocalDate.of(2018, 10, 1);
    private static final MessageType MESSAGETYPE = new MessageType.MessageTypeBuilder()
            .setSchemaSet(new SchemaSet("CR_REQ_PT_03.00"))
            .setVersion(new MessageTypeVersion("03.00"))
            .setName(new MessageTypeName(MessageCodes.CPRequest.CODE))
            .setMimeType(new MimeType("text/xml"))
            .build();

    private final Jaxb2Marshaller marshaller;

    public CPRequestOutbound01p12MessageFactory(Jaxb2Marshaller marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public OutboundMessage createOutboundMessage(CPRequestCR cpRequest) {
        var request = new CPRequest01p12(cpRequest).cpRequest();
        var outputStream = new ByteArrayOutputStream();
        var result = new StreamResult(outputStream);
        marshaller.marshal(request, result);
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        return OutboundMessage.newBuilder()
                              .setInputStream(inputStream)
                              .setOutboundMetaData(outboundMetaData(cpRequest))
                              .build();
    }

    public OutboundMetaData outboundMetaData(CPRequestCR cpRequest) {
        return OutboundMetaData
                .newBuilder()
                .setSenderId(new SenderId(cpRequest.eligiblePartyId()))
                .setReceiverId(new ReceiverId(cpRequest.dsoId()))
                .setMessageType(MESSAGETYPE)
                .build();
    }

    @Override
    public boolean isActive(LocalDate date) {
        return !ACTIVE_FROM.isAfter(date);
    }
}
