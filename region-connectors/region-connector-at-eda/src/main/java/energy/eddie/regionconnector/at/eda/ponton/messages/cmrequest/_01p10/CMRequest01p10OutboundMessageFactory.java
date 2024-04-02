package energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest._01p10;

import de.ponton.xp.adapter.api.domainvalues.*;
import de.ponton.xp.adapter.api.messages.OutboundMessage;
import energy.eddie.regionconnector.at.eda.models.MessageCodes;
import energy.eddie.regionconnector.at.eda.ponton.messages.InactivePontonMessageFactoryException;
import energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest.CMRequestOutboundMessageFactory;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;

import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;

@Component
@SuppressWarnings("DuplicatedCode")
public class CMRequest01p10OutboundMessageFactory implements CMRequestOutboundMessageFactory {
    /**
     * The date until which this message is active. After this date the message is not valid anymore.
     * <p>From <a href="https://www.ebutilities.at/schemas/149">ebutilities</a>
     */
    private static final LocalDate ACTIVE_UNTIL = LocalDate.of(2024, 4, 7);
    private static final MessageType MESSAGETYPE = new MessageType.MessageTypeBuilder()
            .setSchemaSet(new SchemaSet("CM_REQ_ONL_01.10"))
            .setVersion(new MessageTypeVersion("01.10"))
            .setName(new MessageTypeName(MessageCodes.Request.CODE))
            .setMimeType(new MimeType(
                    "text/xml"))
            .build();
    private final Jaxb2Marshaller marshaller;

    public CMRequest01p10OutboundMessageFactory(Jaxb2Marshaller marshaller) throws InactivePontonMessageFactoryException {
        this.marshaller = marshaller;
        if (!isActive(LocalDate.now(AT_ZONE_ID))) {
            throw new InactivePontonMessageFactoryException(CMRequest01p10OutboundMessageFactory.class);
        }
    }

    @Override
    public boolean isActive(LocalDate date) {
        return ACTIVE_UNTIL.isAfter(date) || ACTIVE_UNTIL.isEqual(date);
    }

    @Override
    public OutboundMessage createOutboundMessage(CCMORequest ccmoRequest) {
        var request = new CMRequest01p10(ccmoRequest).cmRequest();
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
}
