// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.inbound.ack.cim;

import energy.eddie.aiida.config.AiidaConfiguration;
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
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinMaxEnvelopeAckFormatterStrategyTest {
    private static final UUID AIIDA_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DATA_SOURCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final UUID PERMISSION_ID = UUID.fromString("00213495-bdbf-4497-8695-5d811e45aa64");
    private static final UUID DATA_NEED_ID = UUID.fromString("5dc71d7e-e8cd-4403-a3a8-d3c095c97a12");

    private final ClassLoader classLoader = getClass().getClassLoader();
    private final MinMaxEnvelopeAckFormatterStrategy strategy = new MinMaxEnvelopeAckFormatterStrategy(AIIDA_ID);

    @Mock
    private Permission permission;
    @Mock
    private AiidaLocalDataNeed dataNeed;
    @Mock
    private InboundDataSource inboundDataSource;
    @Mock
    private InboundRecord inboundRecord;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        var builder = JsonMapper.builder();
        new AiidaConfiguration().objectMapperCustomizer().customize(builder);
        objectMapper = builder.build();

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
    void convert_convertsInboundRecordToAcknowledgementEnvelope() throws Exception {
        try (var jsonStream =
                     classLoader.getResourceAsStream("cim/v1_12/min-max-envelope.json")) {

            var payload = new String(
                    Objects.requireNonNull(jsonStream).readAllBytes(),
                    StandardCharsets.UTF_8
            );

            when(inboundRecord.payload()).thenReturn(payload);

            // When
            var envelope = strategy.convert(objectMapper, inboundRecord);

            // Then
            var header = envelope.getMessageDocumentHeader();
            var metaInfo = header.getMetaInformation();
            var asset = metaInfo.getAsset();
            var marketDocument = envelope.getMarketDocument();

            assertAll(
                    () -> assertNotNull(header.getCreationDateTime()),
                    () -> assertEquals("1", metaInfo.getConnectionId()),
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
                    () -> assertEquals("5dc71d7e-e8cd-4403-a3a8-d3c095c97a12",
                                       marketDocument.getReceivedMarketDocumentMRID()),
                    () -> assertEquals("min-max-envelope", marketDocument.getReceivedMarketDocumentType()),
                    () -> assertEquals("MIN_MAX_ENVELOPE",
                                       marketDocument.getReceivedMarketDocumentProcessProcessType()),
                    () -> assertEquals(ZonedDateTime.parse("2026-02-16T10:11:58Z"),
                                       marketDocument.getReceivedMarketDocumentCreatedDateTime()),
                    () -> assertEquals("AT003000", marketDocument.getSenderMarketParticipantMRID().getValue()),
                    () -> assertEquals("NAT", marketDocument.getSenderMarketParticipantMRID().getCodingScheme()),
                    () -> assertEquals("CONNECTING_SYSTEM_OPERATOR",
                                       marketDocument.getSenderMarketParticipantMarketRoleType()),
                    () -> assertEquals("88e0fc2c-4ea7-4850-a736-8b9742757518",
                                       marketDocument.getReceiverMarketParticipantMRID().getValue()),
                    () -> assertEquals("NAT", marketDocument.getReceiverMarketParticipantMRID().getCodingScheme()),
                    () -> assertEquals("FINAL_CUSTOMER", marketDocument.getReceiverMarketParticipantMarketRoleType())
            );
        }
    }
}
