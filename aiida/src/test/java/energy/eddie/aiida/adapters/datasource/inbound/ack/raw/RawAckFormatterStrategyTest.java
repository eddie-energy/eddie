// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.inbound.ack.raw;

import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.api.agnostic.aiida.AiidaAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RawAckFormatterStrategyTest {
    private static final UUID AIIDA_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DATA_SOURCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final UUID PERMISSION_ID = UUID.fromString("00213495-bdbf-4497-8695-5d811e45aa64");
    private static final UUID DATA_NEED_ID = UUID.fromString("5dc71d7e-e8cd-4403-a3a8-d3c095c97a12");

    private final RawAckFormatterStrategy strategy = new RawAckFormatterStrategy(AIIDA_ID);

    @Mock
    private Permission permission;
    @Mock
    private AiidaLocalDataNeed dataNeed;
    @Mock
    private InboundDataSource inboundDataSource;
    @Mock
    private InboundRecord inboundRecord;

    @BeforeEach
    void setUp() {
        when(inboundRecord.dataSource()).thenReturn(inboundDataSource);

        when(inboundDataSource.countryCode()).thenReturn("ES");
        when(inboundDataSource.id()).thenReturn(DATA_SOURCE_ID);
        when(inboundDataSource.asset()).thenReturn(AiidaAsset.CONNECTION_AGREEMENT_POINT);
        when(inboundDataSource.meterId()).thenReturn("test-meter-id");
        when(inboundDataSource.operatorId()).thenReturn("test-operator-id");
        when(inboundDataSource.permission()).thenReturn(permission);

        when(permission.id()).thenReturn(PERMISSION_ID);
        when(permission.dataNeed()).thenReturn(dataNeed);

        when(dataNeed.dataNeedId()).thenReturn(DATA_NEED_ID);
    }

    @Test
    void convert_convertsInboundRecordToAcknowledgementEnvelope() {
        var payload = "{\"greeting\": \"Hello World!\"}";

        when(inboundRecord.payload()).thenReturn(payload);

        // When
        var envelope = strategy.convert(mock(ObjectMapper.class), inboundRecord);

        // Then
        var header = envelope.getMessageDocumentHeader();
        var metaInfo = header.getMetaInformation();
        var asset = metaInfo.getAsset();
        var marketDocument = envelope.getMarketDocument();

        assertAll(
                () -> assertNotNull(header.getCreationDateTime()),
                () -> assertNull(metaInfo.getConnectionId()),
                () -> assertEquals(PERMISSION_ID.toString(), metaInfo.getRequestPermissionId()),
                () -> assertEquals(DATA_NEED_ID.toString(), metaInfo.getDataNeedId()),
                () -> assertEquals("acknowledgement-market-document", metaInfo.getDocumentType()),
                () -> assertEquals(AIIDA_ID.toString(), metaInfo.getFinalCustomerId()),
                () -> assertEquals(DATA_SOURCE_ID.toString(), metaInfo.getDataSourceId()),
                () -> assertEquals("ES", metaInfo.getRegionCountry()),
                () -> assertEquals("aiida", metaInfo.getRegionConnector())
        );

        assertAll(
                () -> assertEquals("CONNECTION-AGREEMENT-POINT", asset.getType()),
                () -> assertEquals("test-meter-id", asset.getMeterId()),
                () -> assertEquals("test-operator-id", asset.getOperatorId())
        );

        assertAll(
                () -> assertNotNull(marketDocument.getCreatedDateTime()),
                () -> assertNotNull(marketDocument.getMRID()),
                () -> assertEquals("7e21cebad7229fdee4ca592ba243686c9ef1b457be6202ff78c3a5b96693956f",
                                   marketDocument.getReceivedMarketDocumentMRID())
        );
    }
}
