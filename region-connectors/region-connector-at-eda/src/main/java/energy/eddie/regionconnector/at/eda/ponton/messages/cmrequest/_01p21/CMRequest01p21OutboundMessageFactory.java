// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest._01p21;

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
public class CMRequest01p21OutboundMessageFactory implements CMRequestOutboundMessageFactory {
    /**
     * The active from date of the message. The message is active from this date.
     * <p>From <a href="https://www.ebutilities.at/schemas/231">ebutilities</a>
     */
    public static final LocalDate ACTIVE_FROM = LocalDate.of(2025, 4, 7);
    public static final LocalDate ACTIVE_UNTIL = LocalDate.of(2026, 4, 12);
    private static final MessageType MESSAGETYPE = new MessageType.MessageTypeBuilder()
            .setSchemaSet(new SchemaSet(MessageCodes.Request.SCHEMA))
            .setVersion(new MessageTypeVersion(MessageCodes.Request.VERSION))
            .setName(new MessageTypeName(MessageCodes.Request.CODE))
            .setMimeType(new MimeType("text/xml"))
            .build();

    private final Jaxb2Marshaller marshaller;

    public CMRequest01p21OutboundMessageFactory(Jaxb2Marshaller marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public OutboundMessage createOutboundMessage(CCMORequest ccmoRequest) {
        var request = new CMRequest01p21(ccmoRequest).cmRequest();
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
        return !date.isBefore(ACTIVE_FROM) && !date.isAfter(ACTIVE_UNTIL);
    }
}
