// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.inbound.ack;

import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.cim.v1_12.ack.Asset;
import energy.eddie.cim.v1_12.ack.MetaInformation;
import jakarta.annotation.Nullable;

import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

public abstract class BaseAckFormatterStrategy implements AckFormatterStrategy {
    protected static final String REGION_CONNECTOR = "aiida";
    protected static final String DOCUMENT_TYPE = "acknowledgement-market-document";
    protected static final ZoneId UTC = ZoneId.of("UTC");

    protected final UUID aiidaId;

    protected BaseAckFormatterStrategy(UUID aiidaId) {
        this.aiidaId = aiidaId;
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

    private Asset toAsset(InboundDataSource dataSource) {
        return new Asset()
                .withType(dataSource.asset().toString())
                .withMeterId(dataSource.meterId())
                .withOperatorId(dataSource.operatorId());
    }
}
