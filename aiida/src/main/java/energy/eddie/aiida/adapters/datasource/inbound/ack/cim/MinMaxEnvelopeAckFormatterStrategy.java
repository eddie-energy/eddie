// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.inbound.ack.cim;

import energy.eddie.aiida.adapters.datasource.inbound.ack.BaseAckFormatterStrategy;
import energy.eddie.aiida.models.record.InboundProcessingResult;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.cim.v1_12.LocalCodingSchemeType;
import energy.eddie.cim.v1_12.StandardMessageTypeList;
import energy.eddie.cim.v1_12.StandardRoleTypeList;
import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import energy.eddie.cim.v1_12.ack.AcknowledgementMarketDocument;
import energy.eddie.cim.v1_12.ack.MessageDocumentHeader;
import energy.eddie.cim.v1_12.ack.PartyIDString;
import tools.jackson.databind.ObjectMapper;

import java.nio.ByteBuffer;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.UUID;

public class MinMaxEnvelopeAckFormatterStrategy extends BaseAckFormatterStrategy {
    public MinMaxEnvelopeAckFormatterStrategy(UUID aiidaId) {
        super(aiidaId);
    }

    @Override
    public AcknowledgementEnvelope convert(
            ObjectMapper objectMapper,
            InboundRecord inboundRecord,
            InboundProcessingResult processingResult
    ) {
        try {
            return convertParsed(objectMapper, inboundRecord, processingResult);
        } catch (RuntimeException exception) {
            if (processingResult.status() != InboundProcessingResult.Status.REJECTED) {
                throw exception;
            }
            return fallbackRejection(inboundRecord, processingResult);
        }
    }

    private AcknowledgementEnvelope convertParsed(
            ObjectMapper objectMapper,
            InboundRecord inboundRecord,
            InboundProcessingResult processingResult
    ) {
        var minMaxEnvelope = objectMapper.readValue(
                inboundRecord.payload(),
                energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope.class
        );
        var now = ZonedDateTime.now(UTC);

        var minMaxHeader = minMaxEnvelope.getMessageDocumentHeader();
        var minMaxMetaInformation = minMaxHeader.getMetaInformation();

        var header = new MessageDocumentHeader()
                .withCreationDateTime(now)
                .withMetaInformation(toMetaInformation(inboundRecord.dataSource(),
                                                       minMaxMetaInformation.getConnectionId()));

        var marketDocument = toMarketDocument(now, minMaxEnvelope.getMarketDocument(), processingResult)
                .withReceivedMarketDocumentCreatedDateTime(minMaxHeader.getCreationDateTime())
                .withReceivedMarketDocumentType(StandardMessageTypeList.ACKNOWLEDGEMENT_DOCUMENT.value());

        return new AcknowledgementEnvelope()
                .withMessageDocumentHeader(header)
                .withMarketDocument(marketDocument);
    }

    private AcknowledgementEnvelope fallbackRejection(
            InboundRecord inboundRecord,
            InboundProcessingResult processingResult
    ) {
        var now = ZonedDateTime.now(UTC);
        var header = new MessageDocumentHeader()
                .withCreationDateTime(now)
                .withMetaInformation(toMetaInformation(inboundRecord.dataSource(), null));
        var marketDocument = new AcknowledgementMarketDocument()
                .withMRID(UUID.randomUUID().toString())
                .withCreatedDateTime(now)
                .withSenderMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(LocalCodingSchemeType.AIIDA.value())
                                .withValue(truncateUUID(aiidaId))
                )
                .withSenderMarketParticipantMarketRoleType(StandardRoleTypeList.CONSUMER.value())
                .withReceiverMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(LocalCodingSchemeType.AIIDA.value())
                                .withValue("")
                )
                .withReceivedMarketDocumentType(StandardMessageTypeList.ACKNOWLEDGEMENT_DOCUMENT.value())
                .withReasons(toReason(processingResult));

        return new AcknowledgementEnvelope()
                .withMessageDocumentHeader(header)
                .withMarketDocument(marketDocument);
    }

    private static String truncateUUID(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[8]);
        buffer.putLong(uuid.getMostSignificantBits());
        return Base64.getUrlEncoder().encodeToString(buffer.array());
    }

    private AcknowledgementMarketDocument toMarketDocument(
            ZonedDateTime now,
            energy.eddie.cim.v1_12.recmmoe.RECMMOEMarketDocument marketDocument,
            InboundProcessingResult processingResult
    ) {
        return new AcknowledgementMarketDocument()
                .withMRID(UUID.randomUUID().toString())
                .withCreatedDateTime(now)
                .withSenderMarketParticipantMRID(toPartyIdString(marketDocument.getReceiverMarketParticipantMRID()))
                .withSenderMarketParticipantMarketRoleType(marketDocument.getReceiverMarketParticipantMarketRoleType())
                .withReceiverMarketParticipantMRID(toPartyIdString(marketDocument.getSenderMarketParticipantMRID()))
                .withReceiverMarketParticipantMarketRoleType(marketDocument.getSenderMarketParticipantMarketRoleType())
                .withReceivedMarketDocumentMRID(marketDocument.getMRID())
                .withReceivedMarketDocumentRevisionNumber(marketDocument.getRevisionNumber())
                .withReceivedMarketDocumentProcessProcessType(marketDocument.getProcessProcessType())
                .withReasons(toReason(processingResult));
    }

    private PartyIDString toPartyIdString(energy.eddie.cim.v1_12.recmmoe.PartyIDString partyIdString) {
        return new PartyIDString()
                .withValue(partyIdString.getValue())
                .withCodingScheme(partyIdString.getCodingScheme());
    }
}
