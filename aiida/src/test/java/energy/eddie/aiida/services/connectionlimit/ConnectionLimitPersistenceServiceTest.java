// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.connectionlimit;

import energy.eddie.aiida.ObjectMapperCreatorUtil;
import energy.eddie.aiida.aggregator.InboundAggregator;
import energy.eddie.aiida.models.connectionlimit.ConnectionLimit;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.InboundProcessingResult;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.repositories.ConnectionLimitRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.services.InboundProcessingResultHandler;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.test.publisher.TestPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectionLimitPersistenceServiceTest {

    private static final UUID DATA_SOURCE_ID = UUID.fromString("5ed36996-76ce-45f7-b462-3c06b17a0e71");
    private static final UUID PERMISSION_ID = UUID.fromString("00213495-bdbf-4497-8695-5d811e45aa64");
    private static final String METER_ID = "003114735";

    @Mock
    private InboundAggregator inboundAggregator;
    @Mock
    private ConnectionLimitRepository connectionLimitRepository;
    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private InboundDataSource inboundDataSource;
    @Mock
    private Permission permission;
    @Mock
    private InboundProcessingResultHandler processingResultHandler;
    @Captor
    private ArgumentCaptor<ConnectionLimit> limitCaptor;
    @Captor
    private ArgumentCaptor<List<ConnectionLimit>> limitsCaptor;
    @Captor
    private ArgumentCaptor<InboundProcessingResult> processingResultCaptor;

    private ConnectionLimitPersistenceService service;
    private TestPublisher<InboundRecord> publisher;

    @BeforeEach
    void setUp() {
        publisher = TestPublisher.create();
        when(inboundAggregator.inboundRecordFlux()).thenReturn(publisher.flux());
        service = new ConnectionLimitPersistenceService(inboundAggregator,
                                                        connectionLimitRepository,
                                                        permissionRepository,
                                                        ObjectMapperCreatorUtil.mapper(),
                                                        transactionTemplate,
                                                        processingResultHandler);
        lenient().when(permissionRepository.findInboundByDataSourceId(DATA_SOURCE_ID)).thenReturn(Optional.of(permission));
        lenient().when(inboundDataSource.id()).thenReturn(DATA_SOURCE_ID);
        lenient().when(permission.id()).thenReturn(PERMISSION_ID);
        lenient().when(permission.meterId()).thenReturn(METER_ID);
        service.subscribeToInboundRecords();
    }

    @Test
    void subscribedMinMaxDocument_publishesProcessingResult() {
        when(connectionLimitRepository.findMaxRevisionNumberByMrid("document-1")).thenReturn(Optional.empty());
        when(connectionLimitRepository.findCreatedAtByPermissionMeterAndInterval(any(),
                                                                                 any(),
                                                                                 any(),
                                                                                 any())).thenReturn(Optional.empty());
        var inboundRecord = inboundRecord(payload("document-1", "1", "2.0", "8.0", "3.0", "7.0"));

        publisher.next(inboundRecord);

        verify(processingResultHandler, timeout(1000)).handle(inboundRecord, InboundProcessingResult.accepted());
    }

    @Test
    void givenNewDocument_persistsOneEntityPerPoint() {
        when(connectionLimitRepository.findMaxRevisionNumberByMrid("document-1")).thenReturn(Optional.empty());
        when(connectionLimitRepository.findCreatedAtByPermissionMeterAndInterval(any(),
                                                                                 any(),
                                                                                 any(),
                                                                                 any())).thenReturn(Optional.empty());

        service.process(inboundRecord(payload("document-1", "1", "2.0", "8.0", "3.0", "7.0")));

        verify(connectionLimitRepository, times(2)).save(limitCaptor.capture());
        var saved = limitCaptor.getAllValues();

        var first = saved.getFirst();
        assertEquals(PERMISSION_ID, first.permissionId());
        assertEquals(METER_ID, first.meterId());
        assertEquals(Instant.parse("2026-06-01T00:00:00Z"), first.intervalStart());
        assertEquals(Instant.parse("2026-06-01T00:15:00Z"), first.intervalEnd());
        assertEquals(new BigDecimal("2.0"), first.minLimitKw());
        assertEquals(new BigDecimal("8.0"), first.maxLimitKw());
        assertEquals("document-1", first.mrid());
        assertEquals(1, first.revisionNumber());
        assertEquals(Instant.parse("2026-02-16T10:11:58Z"), first.createdAt());
    }

    @Test
    void givenMissingAssetMeterId_fallsBackToPermissionMeterId() {
        when(connectionLimitRepository.findMaxRevisionNumberByMrid("document-1")).thenReturn(Optional.empty());
        when(connectionLimitRepository.findCreatedAtByPermissionMeterAndInterval(any(),
                                                                                 any(),
                                                                                 any(),
                                                                                 any())).thenReturn(Optional.empty());

        service.process(inboundRecord(payload("",
                                              "document-1",
                                              "1",
                                              "2.0",
                                              "8.0",
                                              "3.0",
                                              "7.0",
                                              "2026-02-16T10:11:58Z")));

        verify(connectionLimitRepository, times(2)).save(limitCaptor.capture());
        assertEquals(METER_ID, limitCaptor.getAllValues().getFirst().meterId());
    }

    @Test
    void givenSameMridAndHigherRevision_replacesAllLimitsOfThatMrid() {
        doAnswer(invocation -> {
            Consumer<TransactionStatus> consumer = invocation.getArgument(0);
            consumer.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        when(connectionLimitRepository.findMaxRevisionNumberByMrid("document-1")).thenReturn(Optional.of(1));

        service.process(inboundRecord(payload("document-1", "2", "4.0", "9.0", "5.0", "10.0")));

        verify(connectionLimitRepository).deleteByMrid("document-1");
        verify(connectionLimitRepository).saveAll(limitsCaptor.capture());
        assertEquals(2, limitsCaptor.getValue().size());
        verify(connectionLimitRepository, never()).save(any(ConnectionLimit.class));
    }

    @Test
    void givenSameMridAndLowerOrSameRevision_rejectsDocument() {
        when(connectionLimitRepository.findMaxRevisionNumberByMrid("document-1")).thenReturn(Optional.of(2));

        var inboundRecord = inboundRecord(payload("document-1", "2", "4.0", "9.0", "5.0", "10.0"));
        service.process(inboundRecord);
        var result = handledResult(inboundRecord);

        verify(connectionLimitRepository, never()).deleteByMrid(anyString());
        verify(connectionLimitRepository, never()).save(any(ConnectionLimit.class));
        verify(connectionLimitRepository, never()).saveAll(anyList());
        assertEquals(InboundProcessingResult.Status.REJECTED, result.status());
        assertEquals("Revision 2 is not newer than the latest processed revision 2 for mRID document-1",
                     result.reason());
    }

    @Test
    void givenMismatchingPermissionId_rejectsDocumentWithBothIds() {
        var documentPermissionId = UUID.fromString("11213495-bdbf-4497-8695-5d811e45aa64");
        var inboundRecord = inboundRecord(
                payload("document-1", "1", "2.0", "8.0", "3.0", "7.0")
                        .replace(PERMISSION_ID.toString(), documentPermissionId.toString())
        );
        service.process(inboundRecord);
        var result = handledResult(inboundRecord);
        assertEquals(InboundProcessingResult.Status.REJECTED, result.status());
        assertEquals(
                "Rejected connection limit document for permission %s and mRID document-1: "
                        .formatted(PERMISSION_ID)
                + "Document permission id %s does not match receiving permission id %s"
                        .formatted(documentPermissionId, PERMISSION_ID),
                result.reason()
        );
    }

    @Test
    void givenMismatchingMeterId_rejectsDocumentWithBothIds() {
        var documentMeterId = "different-meter";
        var inboundRecord = inboundRecord(payload(documentMeterId,
                                                  "document-1",
                                                  "1",
                                                  "2.0",
                                                  "8.0",
                                                  "3.0",
                                                  "7.0",
                                                  "2026-02-16T10:11:58Z"));
        service.process(inboundRecord);
        var result = handledResult(inboundRecord);
        assertEquals(InboundProcessingResult.Status.REJECTED, result.status());
        assertEquals(
                "Rejected connection limit document for permission %s and mRID document-1: "
                        .formatted(PERMISSION_ID)
                + "Document meter id %s does not match permission meter id %s"
                        .formatted(documentMeterId, METER_ID),
                result.reason()
        );
    }

    @Test
    void givenNewMridAndNewerCreatedDateTime_updatesExistingInterval() {
        when(connectionLimitRepository.findMaxRevisionNumberByMrid("document-2")).thenReturn(Optional.empty());
        when(connectionLimitRepository.findCreatedAtByPermissionMeterAndInterval(PERMISSION_ID,
                                                                                 METER_ID,
                                                                                 Instant.parse("2026-06-01T00:00:00Z"),
                                                                                 Instant.parse("2026-06-01T00:15:00Z")))
                .thenReturn(Optional.of(Instant.parse("2026-02-16T09:11:58Z")));
        when(connectionLimitRepository.findCreatedAtByPermissionMeterAndInterval(PERMISSION_ID,
                                                                                 METER_ID,
                                                                                 Instant.parse("2026-06-01T00:15:00Z"),
                                                                                 Instant.parse("2026-06-01T00:30:00Z")))
                .thenReturn(Optional.empty());

        service.process(inboundRecord(payload("document-2", "1", "4.0", "9.0", "5.0", "10.0")));

        verify(connectionLimitRepository, times(2)).save(limitCaptor.capture());
        var saved = limitCaptor.getAllValues().getFirst();
        assertEquals(new BigDecimal("4.0"), saved.minLimitKw());
        assertEquals(new BigDecimal("9.0"), saved.maxLimitKw());
        assertEquals("document-2", saved.mrid());
        assertEquals(1, saved.revisionNumber());
        assertEquals(Instant.parse("2026-02-16T10:11:58Z"), saved.createdAt());
    }

    @Test
    void givenNewMridAndOlderCreatedDateTime_rejectsPartialUpdate() {
        when(connectionLimitRepository.findMaxRevisionNumberByMrid("document-2")).thenReturn(Optional.empty());
        when(connectionLimitRepository.findCreatedAtByPermissionMeterAndInterval(PERMISSION_ID,
                                                                                 METER_ID,
                                                                                 Instant.parse("2026-06-01T00:00:00Z"),
                                                                                 Instant.parse("2026-06-01T00:15:00Z")))
                .thenReturn(Optional.of(Instant.parse("2026-02-16T11:11:58Z")));
        when(connectionLimitRepository.findCreatedAtByPermissionMeterAndInterval(PERMISSION_ID,
                                                                                 METER_ID,
                                                                                 Instant.parse("2026-06-01T00:15:00Z"),
                                                                                 Instant.parse("2026-06-01T00:30:00Z")))
                .thenReturn(Optional.empty());

        var inboundRecord = inboundRecord(payload("document-2", "1", "4.0", "9.0", "5.0", "10.0"));
        service.process(inboundRecord);
        var result = handledResult(inboundRecord);

        verify(connectionLimitRepository).save(any(ConnectionLimit.class));
        assertEquals(InboundProcessingResult.Status.PARTIALLY_ACCEPTED, result.status());
        assertEquals("1 of 2 connection limit points were ignored because newer or equally recent values already exist",
                     result.reason());
    }

    @ParameterizedTest
    @MethodSource
    void givenInvalidMinMaxFields_doesNotPersist(String payload) {
        service.process(inboundRecord(payload));

        verify(connectionLimitRepository, never()).save(any());
        verify(connectionLimitRepository, never()).saveAll(anyList());
        verify(connectionLimitRepository, never()).deleteByMrid(anyString());
    }

    private static Stream<Arguments> givenInvalidMinMaxFields_doesNotPersist() {
        return Stream.of(argumentSet("Empty mRID",
                                     payload("", "1", "2.0", "8.0", "3.0", "7.0")),
                         argumentSet("No integer revision number",
                                     payload("document-1", "abc", "2.0", "8.0", "3.0", "7.0")),
                         argumentSet("Invalid revision number",
                                     payload("document-1", "0", "2.0", "8.0", "3.0", "7.0")),
                         argumentSet("Empty creationDateTime",
                                     payload(METER_ID, "document-1", "1", "2.0", "8.0", "3.0", "7.0", ""))
        );
    }

    private InboundRecord inboundRecord(String payload) {
        return new InboundRecord(Instant.parse("2026-06-01T00:00:00Z"),
                                 inboundDataSource,
                                 AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12,
                                 payload);
    }

    private InboundProcessingResult handledResult(InboundRecord inboundRecord) {
        verify(processingResultHandler).handle(eq(inboundRecord), processingResultCaptor.capture());
        return processingResultCaptor.getValue();
    }

    private static String payload(String mrid, String revision, String min1, String max1, String min2, String max2) {
        return payload(METER_ID, mrid, revision, min1, max1, min2, max2, "2026-02-16T10:11:58Z");
    }

    private static String payload(
            String meterId,
            String mrid,
            String revision,
            String min1,
            String max1,
            String min2,
            String max2,
            String creationDateTime
    ) {

        var intervalStart = "2026-06-01T00:00:00Z";
        var intervalEnd = "2026-06-01T01:00:00Z";
        return """
                {
                  "MessageDocumentHeader": {
                    "creationDateTime": "%s",
                    "MetaInformation": {
                      "requestPermissionId": "00213495-bdbf-4497-8695-5d811e45aa64",
                      "Asset": { "meterId": "%s" }
                    }
                  },
                  "MarketDocument": {
                    "mRID": "%s",
                    "revisionNumber": "%s",
                    "sender_MarketParticipant.marketRole.type": "A56",
                    "receiver_MarketParticipant.marketRole.type": "A13",
                    "TimeSeries_Series": [
                      {
                        "Series": [
                          {
                            "Period": [
                              {
                                "resolution": "PT15M",
                                "timeInterval": { "start": "%s", "end": "%s" },
                                "Point": [
                                  { "position": 1, "min_Quantity.quantity": %s, "max_Quantity.quantity": %s },
                                  { "position": 2, "min_Quantity.quantity": %s, "max_Quantity.quantity": %s }
                                ]
                              }
                            ]
                          }
                        ]
                      }
                    ]
                  }
                }
                """.formatted(creationDateTime,
                              meterId,
                              mrid,
                              revision,
                              intervalStart,
                              intervalEnd,
                              min1,
                              max1,
                              min2,
                              max2);
    }
}
