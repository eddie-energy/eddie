// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.aggregator;

import energy.eddie.aiida.adapters.datasource.inbound.InboundAdapter;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.repositories.InboundRecordRepository;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.registry.DefaultHealthContributorRegistry;
import org.springframework.boot.health.registry.HealthContributorRegistry;
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
        Mockito.when(inboundDataSource.id()).thenReturn(DATA_SOURCE_ID);
        aggregator = new InboundAggregator(mockInboundRecordRepository, healthContributorRegistry);
    }

    @Test
    void givenInboundDataSource_savedToInboundRepository() {
        TestPublisher<AiidaRecord> recordPublisher = TestPublisher.create();
        TestPublisher<InboundRecord> inboundPublisher = TestPublisher.create();

        when(inboundAdapter.dataSource()).thenReturn(inboundDataSource);
        when(inboundAdapter.inboundRecordFlux()).thenReturn(inboundPublisher.flux());

        aggregator.addNewDataSourceAdapter(inboundAdapter);

        var inboundRecord = new InboundRecord(Instant.now(),
                                              inboundDataSource,
                                              AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12,
                                              "Test");
        inboundPublisher.next(inboundRecord);
        inboundPublisher.complete();
        recordPublisher.complete();

        verify(mockInboundRecordRepository, times(1)).save(any(InboundRecord.class));
    }
}
