// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.inbound.ack.opaque;

import energy.eddie.aiida.adapters.datasource.inbound.ack.BaseAckFormatterStrategy;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.api.agnostic.opaque.OpaqueEnvelope;
import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import energy.eddie.cim.v1_12.ack.AcknowledgementMarketDocument;
import energy.eddie.cim.v1_12.ack.MessageDocumentHeader;
import tools.jackson.databind.ObjectMapper;

import java.time.ZonedDateTime;
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
                .withReceivedMarketDocumentCreatedDateTime(opaqueEnvelope.timestamp())
                .withReceivedMarketDocumentMRID(opaqueEnvelope.messageId());
    }
}
