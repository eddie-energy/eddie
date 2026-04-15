// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.inbound.ack.cim;

import energy.eddie.aiida.adapters.datasource.inbound.ack.BaseAckFormatterStrategy;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.cim.v1_12.StandardMessageTypeList;
import energy.eddie.cim.v1_12.StandardReasonCodeTypeList;
import energy.eddie.cim.v1_12.ack.*;
import tools.jackson.databind.ObjectMapper;

import java.time.ZonedDateTime;
import java.util.UUID;

public class MinMaxEnvelopeAckFormatterStrategy extends BaseAckFormatterStrategy {
    public MinMaxEnvelopeAckFormatterStrategy(UUID aiidaId) {
        super(aiidaId);
    }

    @Override
    public AcknowledgementEnvelope convert(ObjectMapper objectMapper, InboundRecord inboundRecord) {
        var minMaxEnvelope = objectMapper.readValue(inboundRecord.payload(),
                                                    energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope.class);
        var now = ZonedDateTime.now(UTC);

        var minMaxHeader = minMaxEnvelope.getMessageDocumentHeader();
        var minMaxMetaInformation = minMaxHeader.getMetaInformation();

        var header = new MessageDocumentHeader()
                .withCreationDateTime(now)
                .withMetaInformation(toMetaInformation(inboundRecord.dataSource(),
                                                       minMaxMetaInformation.getConnectionId()));

        var marketDocument = toMarketDocument(now, minMaxEnvelope.getMarketDocument())
                .withReceivedMarketDocumentCreatedDateTime(minMaxHeader.getCreationDateTime())
                .withReceivedMarketDocumentType(StandardMessageTypeList.ACKNOWLEDGEMENT_DOCUMENT.value());

        return new AcknowledgementEnvelope()
                .withMessageDocumentHeader(header)
                .withMarketDocument(marketDocument);
    }


    private AcknowledgementMarketDocument toMarketDocument(
            ZonedDateTime now,
            energy.eddie.cim.v1_12.recmmoe.RECMMOEMarketDocument marketDocument
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
                .withReasons(
                        new Reason()
                                .withCode(StandardReasonCodeTypeList.MESSAGE_FULLY_ACCEPTED.value())
                );
    }

    private PartyIDString toPartyIdString(energy.eddie.cim.v1_12.recmmoe.PartyIDString partyIdString) {
        return new PartyIDString()
                .withValue(partyIdString.getValue())
                .withCodingScheme(partyIdString.getCodingScheme());
    }
}
