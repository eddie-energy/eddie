// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.inbound.ack.opaque;

import energy.eddie.aiida.adapters.datasource.inbound.ack.BaseAckFormatterStrategy;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.cim.agnostic.OpaqueEnvelope;
import energy.eddie.cim.v1_12.LocalCodingSchemeType;
import energy.eddie.cim.v1_12.StandardDocumentTypeList;
import energy.eddie.cim.v1_12.StandardReasonCodeTypeList;
import energy.eddie.cim.v1_12.StandardRoleTypeList;
import energy.eddie.cim.v1_12.ack.*;
import tools.jackson.databind.ObjectMapper;

import java.nio.ByteBuffer;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.UUID;

public class OpaqueAckFormatterStrategy extends BaseAckFormatterStrategy {
    public OpaqueAckFormatterStrategy(UUID aiidaId) {
        super(aiidaId);
    }

    @Override
    public AcknowledgementEnvelope convert(ObjectMapper objectMapper, InboundRecord inboundRecord) {
        var payload = inboundRecord.payload();
        var opaqueEnvelope = objectMapper.readValue(payload, OpaqueEnvelope.class);
        var now = ZonedDateTime.now(UTC);

        var header = new MessageDocumentHeader()
                .withCreationDateTime(now)
                .withMetaInformation(toMetaInformation(inboundRecord.dataSource(), opaqueEnvelope.connectionId()));

        return new AcknowledgementEnvelope()
                .withMessageDocumentHeader(header)
                .withMarketDocument(toMarketDocument(now, opaqueEnvelope));
    }

    private AcknowledgementMarketDocument toMarketDocument(ZonedDateTime now, OpaqueEnvelope opaqueEnvelope) {
        return new AcknowledgementMarketDocument()
                .withMRID(UUID.randomUUID().toString())
                .withCreatedDateTime(now)
                .withSenderMarketParticipantMRID(new PartyIDString()
                                                         .withCodingScheme(LocalCodingSchemeType.AIIDA.value())
                                                         .withValue(truncateUUID(aiidaId))
                )
                .withSenderMarketParticipantMarketRoleType(StandardRoleTypeList.CONSUMER.value())
                .withReceiverMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(LocalCodingSchemeType.AIIDA.value())
                                .withValue("") // Unknown, since the received document does not contain that information
                )
                .withReceivedMarketDocumentCreatedDateTime(opaqueEnvelope.timestamp())
                .withReceivedMarketDocumentMRID(opaqueEnvelope.messageId())
                .withReceivedMarketDocumentType(StandardDocumentTypeList.ACKNOWLEDGEMENT_DOCUMENT.value())
                .withReasons(
                        new Reason()
                                .withCode(StandardReasonCodeTypeList.MESSAGE_FULLY_ACCEPTED.value())
                );
    }

    private static String truncateUUID(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[8]);
        bb.putLong(uuid.getMostSignificantBits());
        return Base64.getUrlEncoder().encodeToString(bb.array());
    }
}
