// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.aggregator.InboundAggregator;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.streamers.StreamerManager;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.cim.agnostic.OpaqueEnvelope;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.publisher.TestPublisher;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InboundPermissionRelayServiceTest {
    private static final UUID INBOUND_DATA_SOURCE_ID = UUID.fromString("5ed36996-76ce-45f7-b462-3c06b17a0e71");
    private static final UUID OUTBOUND_PERMISSION_ID = UUID.fromString("6e0af81e-8f3f-47f5-b74f-3acc54d16673");
    private static final UUID OUTBOUND_DATA_NEED_ID = UUID.fromString("21f7ddd9-a6d3-4742-8ba0-bde66f68f8d7");
    private static final String OUTBOUND_CONNECTION_ID = "outbound-connection";

    @Mock
    private InboundAggregator inboundAggregator;
    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private StreamerManager streamerManager;
    @Mock
    private Permission outboundPermission;
    @Mock
    private AiidaLocalDataNeed outboundDataNeed;
    @Mock
    private InboundDataSource inboundDataSource;

    private ObjectMapper objectMapper;
    private InboundPermissionRelayService relayService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        relayService = new InboundPermissionRelayService(inboundAggregator,
                                                         permissionRepository,
                                                         streamerManager,
                                                         objectMapper);
    }

    @Test
    void givenOpaqueInboundRecord_relaysWithOutboundMetadata() {
        // Given
        var inboundPublisher = TestPublisher.<InboundRecord>create();
        when(inboundAggregator.inboundRecordFlux()).thenReturn(inboundPublisher.flux());
        when(inboundDataSource.id()).thenReturn(INBOUND_DATA_SOURCE_ID);
        mockOutboundPermissionWithSchemas(Set.of(AiidaSchema.OPAQUE));
        when(outboundPermission.connectionId()).thenReturn(OUTBOUND_CONNECTION_ID);
        when(outboundDataNeed.dataNeedId()).thenReturn(OUTBOUND_DATA_NEED_ID);
        when(permissionRepository.findOutboundByDataSourceIdAndStatus(INBOUND_DATA_SOURCE_ID, PermissionStatus.ACTIVE))
                .thenReturn(List.of(outboundPermission));

        relayService.subscribeToInboundRecords();
        var inboundPayload = """
                {
                  "regionConnectorId":"aiida",
                  "permissionId":"inbound-permission",
                  "connectionId":"inbound-connection",
                  "dataNeedId":"inbound-data-need",
                  "messageId":"inbound-message",
                  "timestamp":"2026-06-19T12:00:00Z",
                  "payload":"opaque-payload"
                }
                """;
        var inboundRecord = new InboundRecord(Instant.parse("2026-06-19T12:00:00Z"),
                                              inboundDataSource,
                                              AiidaSchema.OPAQUE,
                                              inboundPayload);

        // When
        inboundPublisher.next(inboundRecord);

        // Then
        verify(streamerManager, timeout(1000)).publishSchemaPayload(
                eq(OUTBOUND_PERMISSION_ID),
                eq(AiidaSchema.OPAQUE),
                argThat(payload -> {
                    var opaque = objectMapper.readValue(payload, OpaqueEnvelope.class);
                    return OUTBOUND_PERMISSION_ID.toString().equals(opaque.permissionId()) &&
                           OUTBOUND_CONNECTION_ID.equals(opaque.connectionId()) &&
                           OUTBOUND_DATA_NEED_ID.toString().equals(opaque.dataNeedId()) &&
                           "opaque-payload".equals(opaque.payload());
                }));
    }

    @Test
    void givenMinMaxInboundRecord_relaysWithOutboundMetadata() {
        // Given
        var inboundPublisher = TestPublisher.<InboundRecord>create();
        when(inboundAggregator.inboundRecordFlux()).thenReturn(inboundPublisher.flux());
        when(inboundDataSource.id()).thenReturn(INBOUND_DATA_SOURCE_ID);
        mockOutboundPermissionWithSchemas(Set.of(AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12));
        when(outboundPermission.connectionId()).thenReturn("outbound-connection");
        when(outboundDataNeed.dataNeedId()).thenReturn(OUTBOUND_DATA_NEED_ID);
        when(permissionRepository.findOutboundByDataSourceIdAndStatus(INBOUND_DATA_SOURCE_ID, PermissionStatus.ACTIVE))
                .thenReturn(List.of(outboundPermission));

        relayService.subscribeToInboundRecords();
        var inboundPayload = """
                {
                  "messageDocumentHeader": {
                    "metaInformation": {
                      "requestPermissionId": "inbound-permission",
                      "connectionId": "inbound-connection",
                      "dataNeedId": "inbound-data-need"
                    }
                  },
                  "marketDocument": {
                    "mRID": "market-document-id"
                  }
                }
                """;
        var inboundRecord = new InboundRecord(Instant.parse("2026-06-19T12:00:00Z"),
                                              inboundDataSource,
                                              AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12,
                                              inboundPayload);

        // When
        inboundPublisher.next(inboundRecord);

        // Then
        verify(streamerManager, timeout(1000)).publishSchemaPayload(
                eq(OUTBOUND_PERMISSION_ID),
                eq(AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12),
                argThat(payload -> {
                    var minmax = objectMapper.readValue(payload, RECMMOEEnvelope.class);
                    var meta = minmax.getMessageDocumentHeader().getMetaInformation();
                    return OUTBOUND_PERMISSION_ID.toString().equals(meta.getRequestPermissionId()) &&
                           OUTBOUND_CONNECTION_ID.equals(meta.getConnectionId()) &&
                           OUTBOUND_DATA_NEED_ID.toString().equals(meta.getDataNeedId());
                }));
    }

    @Test
    void givenInboundRecordWithNonRequestedSchema_doesNotRelay() {
        // Given
        var inboundPublisher = TestPublisher.<InboundRecord>create();
        when(inboundAggregator.inboundRecordFlux()).thenReturn(inboundPublisher.flux());

        relayService.subscribeToInboundRecords();
        var inboundRecord = new InboundRecord(Instant.parse("2026-06-19T12:00:00Z"),
                                              inboundDataSource,
                                              AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12,
                                              "{\"messageDocumentHeader\":{\"metaInformation\":{}}}");

        // When
        inboundPublisher.next(inboundRecord);

        // Then
        verify(streamerManager, never()).publishSchemaPayload(eq(OUTBOUND_PERMISSION_ID),
                                                              eq(AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12),
                                                              anyString());
    }

    private void mockOutboundPermissionWithSchemas(Set<AiidaSchema> schemas) {
        when(outboundPermission.id()).thenReturn(OUTBOUND_PERMISSION_ID);
        when(outboundPermission.dataNeed()).thenReturn(outboundDataNeed);
        when(outboundDataNeed.schemas()).thenReturn(schemas);
    }
}
