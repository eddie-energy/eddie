// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.inbound.ack;

import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.record.InboundProcessingResult;
import energy.eddie.aiida.services.ApplicationInformationService;
import energy.eddie.cim.v1_12.StandardReasonCodeTypeList;
import energy.eddie.cim.v1_12.ack.Asset;
import energy.eddie.cim.v1_12.ack.MetaInformation;
import energy.eddie.cim.v1_12.ack.Reason;
import jakarta.annotation.Nullable;
import tools.jackson.databind.ObjectMapper;

import java.nio.ByteBuffer;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public abstract class BaseAckFormatterStrategy implements AckFormatterStrategy {
    protected static final String REGION_CONNECTOR = "aiida";
    protected static final String DOCUMENT_TYPE = "acknowledgement-market-document";
    protected static final ZoneId UTC = ZoneId.of("UTC");
    private static final int MAX_REASON_TEXT_LENGTH = 512;

    protected final UUID aiidaId;
    protected final ObjectMapper objectMapper;

    protected BaseAckFormatterStrategy(
            ApplicationInformationService applicationInformationService,
            ObjectMapper objectMapper
    ) {
        this.aiidaId = applicationInformationService.applicationInformation().aiidaId();
        this.objectMapper = objectMapper;
    }

    protected MetaInformation toMetaInformation(InboundDataSource dataSource, @Nullable String connectionId) {
        var permission = Objects.requireNonNull(dataSource.permission());
        var dataNeed = Objects.requireNonNull(permission.dataNeed());

        return new MetaInformation()
                .withAsset(toAsset(dataSource))
                .withConnectionId(connectionId)
                .withDataNeedId(dataNeed.dataNeedId().toString())
                .withDataSourceId(dataSource.id().toString())
                .withDocumentType(DOCUMENT_TYPE)
                .withFinalCustomerId(aiidaId.toString())
                .withRequestPermissionId(permission.id().toString())
                .withRegionConnector(REGION_CONNECTOR)
                .withRegionCountry(dataSource.countryCode());
    }

    protected Reason toReason(InboundProcessingResult processingResult) {
        var reason = new Reason().withCode(switch (processingResult.status()) {
            case ACCEPTED -> StandardReasonCodeTypeList.MESSAGE_FULLY_ACCEPTED.value();
            case PARTIALLY_ACCEPTED -> StandardReasonCodeTypeList.MESSAGE_PARTIALLY_ACCEPTED.value();
            case REJECTED -> StandardReasonCodeTypeList.MESSAGE_FULLY_REJECTED.value();
        });

        var text = processingResult.reason();
        if (text != null && !text.isBlank()) {
            reason.withText(text.substring(0, Math.min(text.length(), MAX_REASON_TEXT_LENGTH)));
        }

        return reason;
    }

    protected static String truncateUUID(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[8]);
        buffer.putLong(uuid.getMostSignificantBits());
        return Base64.getUrlEncoder().encodeToString(buffer.array());
    }

    private Asset toAsset(InboundDataSource dataSource) {
        return new Asset()
                .withType(dataSource.asset().toString())
                .withMeterId(dataSource.meterId())
                .withOperatorId(dataSource.operatorId());
    }
}
