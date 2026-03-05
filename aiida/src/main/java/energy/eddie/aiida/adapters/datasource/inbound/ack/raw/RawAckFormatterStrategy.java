// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.inbound.ack.raw;

import energy.eddie.aiida.adapters.datasource.inbound.ack.BaseAckFormatterStrategy;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import energy.eddie.cim.v1_12.ack.AcknowledgementMarketDocument;
import energy.eddie.cim.v1_12.ack.MessageDocumentHeader;
import org.apache.commons.codec.digest.DigestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.ZonedDateTime;
import java.util.UUID;

public class RawAckFormatterStrategy extends BaseAckFormatterStrategy {
    public RawAckFormatterStrategy(UUID aiidaId) {
        super(aiidaId);
    }

    @Override
    public AcknowledgementEnvelope convert(ObjectMapper objectMapper, InboundRecord inboundRecord) {
        var now = ZonedDateTime.now(UTC);

        var header = new MessageDocumentHeader()
                .withCreationDateTime(now)
                .withMetaInformation(toMetaInformation(inboundRecord.dataSource(), null));

        return new AcknowledgementEnvelope()
                .withMessageDocumentHeader(header)
                .withMarketDocument(toMarketDocument(now, inboundRecord.payload()));
    }

    private AcknowledgementMarketDocument toMarketDocument(ZonedDateTime now, String payload) {
        return new AcknowledgementMarketDocument()
                .withMRID(UUID.randomUUID().toString())
                .withCreatedDateTime(now)
                .withReceivedMarketDocumentMRID(hashPayload(payload));
    }

    private String hashPayload(String payload) {
        return DigestUtils.sha256Hex(payload);
    }
}
