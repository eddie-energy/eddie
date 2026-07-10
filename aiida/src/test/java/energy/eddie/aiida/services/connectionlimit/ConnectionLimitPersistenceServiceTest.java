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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.publisher.TestPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectionLimitPersistenceServiceTest {

    private static final UUID PERMISSION_ID = UUID.fromString("00213495-bdbf-4497-8695-5d811e45aa64");
    private static final String METER_ID = "003114735";

    // language=json
    private static final String MIN_MAX_PAYLOAD = """
            {
              "MessageDocumentHeader": {
                "MetaInformation": {
                  "requestPermissionId": "00213495-bdbf-4497-8695-5d811e45aa64",
                  "Asset": { "meterId": "003114735" }
                }
              },
              "MarketDocument": {
                "sender_MarketParticipant.marketRole.type": "A56",
                "receiver_MarketParticipant.marketRole.type": "A13",
                "TimeSeries_Series": [
                  {
                    "Series": [
                      {
                        "Period": [
                          {
                            "resolution": "PT15M",
                            "timeInterval": { "start": "2026-06-01T00:00:00Z", "end": "2026-06-01T01:00:00Z" },
                            "Point": [
                              { "position": 1, "min_Quantity.quantity": 2.0, "max_Quantity.quantity": 8.0 },
                              { "position": 2, "min_Quantity.quantity": 3.0, "max_Quantity.quantity": 7.0 }
                            ]
                          }
                        ]
                      }
                    ]
                  }
                ]
              }
            }
            """;

    @Mock
    private InboundAggregator inboundAggregator;
    @Mock
    private ConnectionLimitRepository connectionLimitRepository;
    @Mock
    private InboundDataSource inboundDataSource;
    @Captor
    private ArgumentCaptor<ConnectionLimit> limitCaptor;

    private ConnectionLimitPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new ConnectionLimitPersistenceService(inboundAggregator,
                                                        connectionLimitRepository,
                                                        ObjectMapperCreatorUtil.mapper());
    }

    @Test
    void givenMinMaxEnvelopeRecord_persistsOneEntityPerPoint() {
        var publisher = TestPublisher.<InboundRecord>create();
        when(inboundAggregator.inboundRecordFlux()).thenReturn(publisher.flux());
        service.subscribeToInboundRecords();

        publisher.next(inboundRecord(AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12, MIN_MAX_PAYLOAD));

        verify(connectionLimitRepository, timeout(1000).times(2)).save(limitCaptor.capture());
        List<ConnectionLimit> saved = limitCaptor.getAllValues();

        var first = saved.getFirst();
        assertEquals(PERMISSION_ID, first.permissionId());
        assertEquals(METER_ID, first.meterId());
        assertEquals(Instant.parse("2026-06-01T00:00:00Z"), first.intervalStart());
        assertEquals(Instant.parse("2026-06-01T00:15:00Z"), first.intervalEnd());
        assertEquals(new BigDecimal("2.0"), first.minLimitKw());
        assertEquals(new BigDecimal("8.0"), first.maxLimitKw());

        var second = saved.get(1);
        assertEquals(Instant.parse("2026-06-01T00:15:00Z"), second.intervalStart());
        assertEquals(Instant.parse("2026-06-01T00:30:00Z"), second.intervalEnd());
        assertEquals(new BigDecimal("3.0"), second.minLimitKw());
        assertEquals(new BigDecimal("7.0"), second.maxLimitKw());
    }

    @Test
    void givenNonMinMaxRecord_doesNotPersist() {
        var publisher = TestPublisher.<InboundRecord>create();
        when(inboundAggregator.inboundRecordFlux()).thenReturn(publisher.flux());
        service.subscribeToInboundRecords();

        publisher.next(inboundRecord(AiidaSchema.OPAQUE, "{}"));

        verify(connectionLimitRepository, after(200).never()).save(any());
    }

    private InboundRecord inboundRecord(AiidaSchema schema, String payload) {
        return new InboundRecord(Instant.parse("2026-06-01T00:00:00Z"), inboundDataSource, schema, payload);
    }
}
