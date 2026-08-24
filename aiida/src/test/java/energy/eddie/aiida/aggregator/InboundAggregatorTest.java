// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.aggregator;

import energy.eddie.aiida.adapters.datasource.inbound.InboundAdapter;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.record.DataSourceRecord;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.repositories.InboundRecordRepository;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Subscription;
import org.springframework.boot.health.registry.DefaultHealthContributorRegistry;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import reactor.core.publisher.BaseSubscriber;
import reactor.test.publisher.TestPublisher;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InboundAggregatorTest {
    private static final UUID DATA_SOURCE_ID = UUID.fromString("6211ea05-d4ab-48ff-8613-8f4791a56606");
    private final HealthContributorRegistry healthContributorRegistry = new DefaultHealthContributorRegistry();
    private InboundAggregator aggregator;
    @Mock
    private InboundDataSource inboundDataSource;
    @Mock
    private InboundAdapter inboundAdapter;
    @Mock
    private InboundRecordRepository mockInboundRecordRepository;

    @BeforeEach
    void setUp() {
        when(inboundDataSource.id()).thenReturn(DATA_SOURCE_ID);
        aggregator = new InboundAggregator(mockInboundRecordRepository, healthContributorRegistry);
    }

    @Test
    void givenInboundDataSource_savedToInboundRepository() {
        TestPublisher<DataSourceRecord> inboundPublisher = TestPublisher.create();

        when(inboundAdapter.dataSource()).thenReturn(inboundDataSource);
        when(inboundAdapter.start())
                .thenReturn(inboundPublisher.flux());
        aggregator.addNewDataSourceAdapter(inboundAdapter);

        var inboundRecord = new InboundRecord(Instant.parse("2026-06-09T12:00:00Z"),
                                              inboundDataSource,
                                              AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12,
                                              "Test");
        inboundPublisher.next(inboundRecord);
        inboundPublisher.complete();

        verify(inboundAdapter, times(1)).start();
        verify(mockInboundRecordRepository, timeout(1000)).save(any(InboundRecord.class));
    }

    @Test
    void givenDemandStarvedInboundRecordFluxSubscriber_stillSavesEveryRecordToInboundRepository() {
        TestPublisher<DataSourceRecord> inboundPublisher = TestPublisher.create();

        when(inboundAdapter.dataSource()).thenReturn(inboundDataSource);
        when(inboundAdapter.start())
                .thenReturn(inboundPublisher.flux());
        aggregator.addNewDataSourceAdapter(inboundAdapter);

        aggregator.inboundRecordFlux().subscribe(new BaseSubscriber<>() {
            @Override
            protected void hookOnSubscribe(@NonNull Subscription subscription) {
                // Intentionally request nothing.
            }
        });

        var recordCount = 5;
        for (var i = 0; i < recordCount; i++) {
            inboundPublisher.next(new InboundRecord(Instant.parse("2026-06-09T12:00:00Z").plusSeconds(i),
                                                     inboundDataSource,
                                                     AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12,
                                                     "Test " + i));
        }
        inboundPublisher.complete();

        verify(mockInboundRecordRepository, timeout(1000).times(recordCount)).save(any(InboundRecord.class));
    }

    @Test
    void givenSaveThrowsForOneRecord_laterRecordsAreStillSavedToInboundRepository() {
        TestPublisher<DataSourceRecord> inboundPublisher = TestPublisher.create();

        when(inboundAdapter.dataSource()).thenReturn(inboundDataSource);
        when(inboundAdapter.start())
                .thenReturn(inboundPublisher.flux());
        when(mockInboundRecordRepository.save(any(InboundRecord.class)))
                .thenThrow(new RuntimeException("Simulated transient save failure"))
                .thenAnswer(invocation -> invocation.getArgument(0));
        aggregator.addNewDataSourceAdapter(inboundAdapter);

        var failingRecord = new InboundRecord(Instant.parse("2026-06-09T12:00:00Z"),
                                              inboundDataSource,
                                              AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12,
                                              "Failing");
        var laterRecord = new InboundRecord(Instant.parse("2026-06-09T12:00:01Z"),
                                            inboundDataSource,
                                            AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12,
                                            "Later");
        inboundPublisher.next(failingRecord);
        inboundPublisher.next(laterRecord);
        inboundPublisher.complete();

        verify(mockInboundRecordRepository, timeout(1000).times(2)).save(any(InboundRecord.class));
    }
}
