// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.inbound.ack.cim;

import energy.eddie.aiida.adapters.datasource.inbound.ack.BaseAckFormatterStrategy;
import energy.eddie.aiida.errors.formatter.MinMaxEnvelopeAcknowledgementFormattingException;
import energy.eddie.aiida.models.record.InboundProcessingResult;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.services.ApplicationInformationService;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.cim.v1_12.LocalCodingSchemeType;
import energy.eddie.cim.v1_12.StandardMessageTypeList;
import energy.eddie.cim.v1_12.StandardRoleTypeList;
import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import energy.eddie.cim.v1_12.ack.AcknowledgementMarketDocument;
import energy.eddie.cim.v1_12.ack.MessageDocumentHeader;
import energy.eddie.cim.v1_12.ack.PartyIDString;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.ZonedDateTime;
import java.util.UUID;

@Component
public class MinMaxEnvelopeAckFormatterStrategy extends BaseAckFormatterStrategy {
    public MinMaxEnvelopeAckFormatterStrategy(
            ApplicationInformationService applicationInformationService,
            ObjectMapper objectMapper
    ) {
        super(applicationInformationService, objectMapper);
    }

    @Override
    public AiidaSchema supportedSchema() {
        return AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12;
    }

    @Override
    public AcknowledgementEnvelope convert(
            InboundRecord inboundRecord,
            InboundProcessingResult processingResult
    ) {
        try {
            return convertParsed(inboundRecord, processingResult);
        } catch (MinMaxEnvelopeAcknowledgementFormattingException exception) {
            if (processingResult.status() == InboundProcessingResult.Status.REJECTED) {
                return fallbackRejection(inboundRecord, processingResult);
            }
            throw exception;
        }
    }

    private AcknowledgementEnvelope convertParsed(
            InboundRecord inboundRecord,
            InboundProcessingResult processingResult
    ) {
        try {
            var minMaxEnvelope = objectMapper.readValue(inboundRecord.payload(), RECMMOEEnvelope.class);
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
        } catch (RuntimeException exception) {
            throw new MinMaxEnvelopeAcknowledgementFormattingException(
                    "Failed to create an acknowledgement from the min-max envelope",
                    exception
            );
        }
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

    private AcknowledgementMarketDocument toMarketDocument(
            ZonedDateTime now,
            energy.eddie.cim.v1_12.recmmoe.RECMMOEMarketDocument marketDocument,
            InboundProcessingResult processingResult
    ) {
        return new AcknowledgementMarketDocument()
                .withMRID(UUID.randomUUID().toString())
                .withCreatedDateTime(now)
                .withSenderMarketParticipantMRID(toAckPartyIdString(marketDocument.getReceiverMarketParticipantMRID()))
                .withSenderMarketParticipantMarketRoleType(marketDocument.getReceiverMarketParticipantMarketRoleType())
                .withReceiverMarketParticipantMRID(toAckPartyIdString(marketDocument.getSenderMarketParticipantMRID()))
                .withReceiverMarketParticipantMarketRoleType(marketDocument.getSenderMarketParticipantMarketRoleType())
                .withReceivedMarketDocumentMRID(marketDocument.getMRID())
                .withReceivedMarketDocumentRevisionNumber(marketDocument.getRevisionNumber())
                .withReceivedMarketDocumentProcessProcessType(marketDocument.getProcessProcessType())
                .withReasons(toReason(processingResult));
    }

    private PartyIDString toAckPartyIdString(energy.eddie.cim.v1_12.recmmoe.PartyIDString partyIdString) {
        return new PartyIDString()
                .withValue(partyIdString.getValue())
                .withCodingScheme(partyIdString.getCodingScheme());
    }
}
