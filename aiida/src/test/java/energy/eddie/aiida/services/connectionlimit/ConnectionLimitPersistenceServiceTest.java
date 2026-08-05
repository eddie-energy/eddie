// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.connectionlimit;

import energy.eddie.aiida.ObjectMapperCreatorUtil;
import energy.eddie.aiida.aggregator.InboundAggregator;
import energy.eddie.aiida.models.connectionlimit.ConnectionLimit;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.repositories.ConnectionLimitRepository;
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

    private static final UUID PERMISSION_ID = UUID.fromString("00213495-bdbf-4497-8695-5d811e45aa64");
    private static final String METER_ID = "003114735";

    @Mock
    private InboundAggregator inboundAggregator;
    @Mock
    private ConnectionLimitRepository connectionLimitRepository;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private InboundDataSource inboundDataSource;
    @Captor
    private ArgumentCaptor<ConnectionLimit> limitCaptor;
    @Captor
    private ArgumentCaptor<List<ConnectionLimit>> limitsCaptor;

    private TestPublisher<InboundRecord> publisher;

    @BeforeEach
    void setUp() {
        var service = new ConnectionLimitPersistenceService(inboundAggregator,
                                                            connectionLimitRepository,
                                                            ObjectMapperCreatorUtil.mapper(),
                                                            transactionTemplate);
        publisher = TestPublisher.create();
        when(inboundAggregator.inboundRecordFlux()).thenReturn(publisher.flux());
        service.subscribeToInboundRecords();
    }

    @Test
    void givenNewDocument_persistsOneEntityPerPoint() {
        when(connectionLimitRepository.findMaxRevisionNumberByMrid("document-1")).thenReturn(Optional.empty());
        when(connectionLimitRepository.findCreatedAtByPermissionMeterAndInterval(any(),
                                                                                 any(),
                                                                                 any(),
                                                                                 any())).thenReturn(Optional.empty());

        publisher.next(inboundRecord(payload("document-1", "1", "2.0", "8.0", "3.0", "7.0")));

        verify(connectionLimitRepository, timeout(1000).times(2)).save(limitCaptor.capture());
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
    void givenSameMridAndHigherRevision_replacesAllLimitsOfThatMrid() {
        doAnswer(invocation -> {
            Consumer<TransactionStatus> consumer = invocation.getArgument(0);
            consumer.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        when(connectionLimitRepository.findMaxRevisionNumberByMrid("document-1")).thenReturn(Optional.of(1));

        publisher.next(inboundRecord(payload("document-1", "2", "4.0", "9.0", "5.0", "10.0")));

        verify(connectionLimitRepository, timeout(1000)).deleteByMrid("document-1");
        verify(connectionLimitRepository, timeout(1000)).saveAll(limitsCaptor.capture());
        assertEquals(2, limitsCaptor.getValue().size());
        verify(connectionLimitRepository, never()).save(any(ConnectionLimit.class));
    }

    @Test
    void givenSameMridAndLowerOrSameRevision_rejectsDocument() {
        when(connectionLimitRepository.findMaxRevisionNumberByMrid("document-1")).thenReturn(Optional.of(2));

        publisher.next(inboundRecord(payload("document-1", "2", "4.0", "9.0", "5.0", "10.0")));

        verify(connectionLimitRepository, after(200).never()).deleteByMrid(anyString());
        verify(connectionLimitRepository, after(200).never()).save(any(ConnectionLimit.class));
        verify(connectionLimitRepository, after(200).never()).saveAll(anyList());
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

        publisher.next(inboundRecord(payload("document-2", "1", "4.0", "9.0", "5.0", "10.0")));

        verify(connectionLimitRepository, timeout(1000).times(2)).save(limitCaptor.capture());
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

        publisher.next(inboundRecord(payload("document-2", "1", "4.0", "9.0", "5.0", "10.0")));

        verify(connectionLimitRepository, timeout(1000).times(1)).save(any(ConnectionLimit.class));
    }

    @Test
    void givenNonMinMaxRecord_doesNotPersist() {
        publisher.next(new InboundRecord(Instant.parse("2026-06-01T00:00:00Z"),
                                         inboundDataSource,
                                         AiidaSchema.OPAQUE,
                                         "{}"));

        verify(connectionLimitRepository, after(200).never()).save(any());
        verify(connectionLimitRepository, after(200).never()).saveAll(anyList());
    }

    @ParameterizedTest
    @MethodSource
    void givenInvalidMinMaxFields_doesNotPersist(String payload) {
        publisher.next(inboundRecord(payload));

        verify(connectionLimitRepository, after(200).never()).save(any());
        verify(connectionLimitRepository, after(200).never()).saveAll(anyList());
        verify(connectionLimitRepository, after(200).never()).deleteByMrid(anyString());
    }

    private static Stream<Arguments> givenInvalidMinMaxFields_doesNotPersist() {
        return Stream.of(argumentSet("Empty mRID",
                                     payload("", "1", "2.0", "8.0", "3.0", "7.0")),
                         argumentSet("No integer revision number",
                                     payload("document-1", "abc", "2.0", "8.0", "3.0", "7.0")),
                         argumentSet("Invalid revision number",
                                     payload("document-1", "0", "2.0", "8.0", "3.0", "7.0")),
                         argumentSet("Empty creationDateTime",
                                     payload("document-1", "1", "2.0", "8.0", "3.0", "7.0", ""))
        );
    }

    private InboundRecord inboundRecord(String payload) {
        return new InboundRecord(Instant.parse("2026-06-01T00:00:00Z"),
                                 inboundDataSource,
                                 AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12,
                                 payload);
    }

    private static String payload(String mrid, String revision, String min1, String max1, String min2, String max2) {
        return payload(mrid, revision, min1, max1, min2, max2, "2026-02-16T10:11:58Z");
    }

    private static String payload(
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
                      "Asset": { "meterId": "003114735" }
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
