// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.schemas.ack;

import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.cim.v1_12.ack.*;
import tools.jackson.databind.ObjectMapper;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

public class MinMaxEnvelopeCimFormatterStrategy implements CimFormatterStrategy {
    protected static final String DOCUMENT_TYPE = "acknowledgement-market-document";
    protected static final ZoneId UTC = ZoneId.of("UTC");

    @Override
    public AcknowledgementEnvelope convert(ObjectMapper objectMapper, InboundRecord inboundRecord) {
        var minMaxEnvelope = objectMapper.readValue(inboundRecord.payload(),
                                                    energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope.class);
        var now = ZonedDateTime.now(UTC);

        var minMaxHeader = minMaxEnvelope.getMessageDocumentHeader();
        var minMaxMetaInformation = minMaxHeader.getMetaInformation();

        var header = new MessageDocumentHeader()
                .withCreationDateTime(now)
                .withMetaInformation(toMetaInformation(minMaxMetaInformation, inboundRecord.dataSource()));

        var marketDocument = toMarketDocument(now, minMaxEnvelope.getMarketDocument())
                .withReceivedMarketDocumentCreatedDateTime(minMaxHeader.getCreationDateTime())
                .withReceivedMarketDocumentType(minMaxMetaInformation.getDocumentType());

        return new AcknowledgementEnvelope()
                .withMessageDocumentHeader(header)
                .withMarketDocument(marketDocument);
    }

    private MetaInformation toMetaInformation(
            energy.eddie.cim.v1_12.recmmoe.MetaInformation metaInformation,
            InboundDataSource dataSource
    ) {
        return new MetaInformation()
                .withAsset(toAsset(dataSource))
                .withConnectionId(metaInformation.getConnectionId())
                .withDataNeedId(metaInformation.getDataNeedId())
                .withDataSourceId(dataSource.id().toString())
                .withDocumentType(DOCUMENT_TYPE)
                .withFinalCustomerId(metaInformation.getFinalCustomerId())
                .withRequestPermissionId(metaInformation.getRequestPermissionId())
                .withRegionConnector(metaInformation.getRegionConnector())
                .withRegionCountry(dataSource.countryCode());
    }

    private Asset toAsset(InboundDataSource dataSource) {
        return new Asset()
                .withType(dataSource.asset().toString())
                .withMeterId(dataSource.meterId())
                .withOperatorId(dataSource.meterId());
    }

    private AcknowledgementMarketDocument toMarketDocument(
            ZonedDateTime now,
            energy.eddie.cim.v1_12.recmmoe.RECMMOEMarketDocument marketDocument
    ) {
        return new AcknowledgementMarketDocument()
                .withMRID(UUID.randomUUID().toString())
                .withCreatedDateTime(now)
                .withSenderMarketParticipantMRID(toPartyIdString(marketDocument.getSenderMarketParticipantMRID()))
                .withSenderMarketParticipantMarketRoleType(marketDocument.getSenderMarketParticipantMarketRoleType())
                .withReceiverMarketParticipantMRID(toPartyIdString(marketDocument.getReceiverMarketParticipantMRID()))
                .withReceiverMarketParticipantMarketRoleType(marketDocument.getReceiverMarketParticipantMarketRoleType())
                .withReceivedMarketDocumentMRID(marketDocument.getMRID())
                .withReceivedMarketDocumentRevisionNumber(marketDocument.getRevisionNumber())
                .withReceivedMarketDocumentProcessProcessType(marketDocument.getProcessProcessType());
    }

    private PartyIDString toPartyIdString(energy.eddie.cim.v1_12.recmmoe.PartyIDString partyIdString) {
        return new PartyIDString()
                .withValue(partyIdString.getValue())
                .withCodingScheme(partyIdString.getCodingScheme());
    }
}
