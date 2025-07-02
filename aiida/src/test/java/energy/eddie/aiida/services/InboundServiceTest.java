package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceMqttDto;
import energy.eddie.aiida.errors.UnauthorizedException;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.repositories.DataSourceRepository;
import energy.eddie.aiida.repositories.InboundRecordRepository;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboundServiceTest {
    private static final UUID DATA_SOURCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String ACCESS_CODE = "test-access-code";
    private static final String ACCESS_CODE_HASH = "$2a$12$CddkrCJh6.JIzsztL1LvDu/ZL79oDC3F9oI32vlfGlHBY/IQEBypm";
    private static final InboundDataSource DATA_SOURCE = new InboundDataSource(
            new DataSourceDto(DATA_SOURCE_ID,
                              DataSourceType.SMART_METER_ADAPTER,
                              AiidaAsset.SUBMETER,
                              "sma",
                              "AT",
                              true,
                              null,
                              null,
                              null),
            USER_ID,
            new DataSourceMqttDto("tcp://localhost:1883",
                                  "tcp://localhost:1883",
                                  "aiida/test",
                                  "user",
                                  "password"),
            ACCESS_CODE_HASH
    );
    private static final InboundRecord INBOUND_RECORD = new InboundRecord(
            Instant.now(),
            AiidaAsset.SUBMETER,
            USER_ID,
            DATA_SOURCE_ID,
            "test"
    );

    @Mock
    private DataSourceRepository dataSourceRepository;
    @Mock
    private InboundRecordRepository inboundRecordRepository;

    private InboundService inboundService;

    @BeforeEach
    void setUp() {
        inboundService = new InboundService(dataSourceRepository, inboundRecordRepository);
    }

    @Test
    void testLatestRecord_returnsRecord() throws UnauthorizedException {
        // Given
        when(dataSourceRepository.findById(DATA_SOURCE_ID)).thenReturn(Optional.of(DATA_SOURCE));
        when(inboundRecordRepository.findTopByDataSourceIdOrderByTimestampDesc(DATA_SOURCE_ID)).thenReturn(Optional.of(INBOUND_RECORD));

        // When
        var inboundRecord = inboundService.latestRecord(ACCESS_CODE, DATA_SOURCE_ID);

        // Then
        assertEquals(DATA_SOURCE_ID, inboundRecord.dataSourceId());
    }

    @Test
    void testLatestRecord_withWrongAccessCode_throwsException() {
        // Given
        when(dataSourceRepository.findById(DATA_SOURCE_ID)).thenReturn(Optional.of(DATA_SOURCE));

        // When, Then
        assertThrows(UnauthorizedException.class, () -> inboundService.latestRecord("wrong", DATA_SOURCE_ID));
    }

    @Test
    void testLatestRecord_withMissingDataSource_throwsException() {
        // Given
        when(dataSourceRepository.findById(DATA_SOURCE_ID)).thenReturn(Optional.empty());

        // When, Then
        assertThrows(EntityNotFoundException.class, () -> inboundService.latestRecord(ACCESS_CODE, DATA_SOURCE_ID));
    }

    @Test
    void testLatestRecord_withMissingRecord_throwsException() {
        // Given
        when(dataSourceRepository.findById(DATA_SOURCE_ID)).thenReturn(Optional.of(DATA_SOURCE));
        when(inboundRecordRepository.findTopByDataSourceIdOrderByTimestampDesc(DATA_SOURCE_ID)).thenReturn(Optional.empty());

        // When, Then
        assertThrows(EntityNotFoundException.class, () -> inboundService.latestRecord(ACCESS_CODE, DATA_SOURCE_ID));
    }
}
