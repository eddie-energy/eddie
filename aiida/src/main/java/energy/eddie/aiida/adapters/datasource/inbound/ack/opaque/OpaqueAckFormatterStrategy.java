// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.inbound.ack.opaque;

import energy.eddie.aiida.adapters.datasource.inbound.ack.BaseAckFormatterStrategy;
import energy.eddie.aiida.models.record.InboundProcessingResult;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.services.ApplicationInformationService;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.cim.agnostic.OpaqueEnvelope;
import energy.eddie.cim.v1_12.LocalCodingSchemeType;
import energy.eddie.cim.v1_12.StandardDocumentTypeList;
import energy.eddie.cim.v1_12.StandardRoleTypeList;
import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import energy.eddie.cim.v1_12.ack.AcknowledgementMarketDocument;
import energy.eddie.cim.v1_12.ack.MessageDocumentHeader;
import energy.eddie.cim.v1_12.ack.PartyIDString;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.ZonedDateTime;
import java.util.UUID;

@Component
public class OpaqueAckFormatterStrategy extends BaseAckFormatterStrategy {
    public OpaqueAckFormatterStrategy(
            ApplicationInformationService applicationInformationService,
            ObjectMapper objectMapper
    ) {
        super(applicationInformationService, objectMapper);
    }

    @Override
    public AiidaSchema supportedSchema() {
        return AiidaSchema.OPAQUE;
    }

    @Override
    public AcknowledgementEnvelope convert(
            InboundRecord inboundRecord,
            InboundProcessingResult processingResult
    ) {
        var opaqueEnvelope = objectMapper.readValue(inboundRecord.payload(), OpaqueEnvelope.class);
        var now = ZonedDateTime.now(UTC);

        var header = new MessageDocumentHeader()
                .withCreationDateTime(now)
                .withMetaInformation(toMetaInformation(inboundRecord.dataSource(), opaqueEnvelope.connectionId()));

        return new AcknowledgementEnvelope()
                .withMessageDocumentHeader(header)
                .withMarketDocument(toMarketDocument(now, opaqueEnvelope, processingResult));
    }

    private AcknowledgementMarketDocument toMarketDocument(
            ZonedDateTime now,
            OpaqueEnvelope opaqueEnvelope,
            InboundProcessingResult processingResult
    ) {
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
                .withReasons(toReason(processingResult));
    }
}
