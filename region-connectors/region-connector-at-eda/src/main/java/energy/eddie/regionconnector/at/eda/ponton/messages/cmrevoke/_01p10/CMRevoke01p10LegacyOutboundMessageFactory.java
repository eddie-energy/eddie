// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

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
@SuppressWarnings("DuplicatedCode")
public class CMRevoke01p10LegacyOutboundMessageFactory implements CMRevokeOutboundMessageFactory {
    public static final LocalDate ACTIVE_FROM = LocalDate.of(2026, 4, 13);
    private final Jaxb2Marshaller marshaller;

    public CMRevoke01p10LegacyOutboundMessageFactory(Jaxb2Marshaller marshaller) {
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
        return !date.isBefore(ACTIVE_FROM) && date.isBefore(CMRevoke01p10OutboundMessageFactory.ACTIVE_FROM);
    }

    private static MessageType createMessageType() {
        return new MessageType.MessageTypeBuilder()
                .setSchemaSet(new SchemaSet(MessageCodes.Revoke.EligibleParty.SCHEMA_LEGACY))
                .setVersion(new MessageTypeVersion(MessageCodes.Revoke.EligibleParty.VERSION_LEGACY))
                .setName(new MessageTypeName(MessageCodes.Revoke.EligibleParty.REVOKE))
                .setMimeType(new MimeType("text/xml"))
                .build();
    }
}