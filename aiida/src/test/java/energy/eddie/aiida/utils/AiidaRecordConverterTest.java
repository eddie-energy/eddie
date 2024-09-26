package energy.eddie.aiida.utils;

import energy.eddie.aiida.dtos.AiidaRecordStreamingDto;
import energy.eddie.aiida.models.permission.AiidaLocalDataNeed;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiidaRecordConverterTest {
    @Mock
    private AiidaLocalDataNeed mockDataNeed;
    @Mock
    private Permission mockPermission;

    @Test
    void givenIntegerAiidaRecord_returnsDtoWithFieldsSet() {
        // Given
        Instant timestamp = Instant.now();
        var aiidaRecord = new AiidaRecord(timestamp, "Test", List.of(
                new AiidaRecordValue("1.8.0", "1.8.0", "23", "kWh", "10", "kWh")));
        when(mockPermission.connectionId()).thenReturn("connectionId");
        when(mockPermission.permissionId()).thenReturn("permissionId");
        when(mockPermission.dataNeed()).thenReturn(mockDataNeed);

        // When
        AiidaRecordStreamingDto dto = AiidaRecordConverter.recordToStreamingDto(aiidaRecord, mockPermission);

        // Then
        assertEquals("10", dto.aiidaRecordValues().getFirst().value());
        assertEquals("23", dto.aiidaRecordValues().getFirst().rawValue());
        assertEquals("1.8.0", dto.aiidaRecordValues().getFirst().rawTag());
        assertEquals("connectionId", dto.connectionId());
        assertEquals("permissionId", dto.permissionId());
        assertEquals(timestamp.toEpochMilli(), dto.timestamp().toEpochMilli());
    }

    @Test
    void givenStringAiidaRecord_returnsDtoWithFieldsSet() {
        // Given
        Instant timestamp = Instant.now();
        var aiidaRecord = new AiidaRecord(timestamp, "Test", List.of(
                new AiidaRecordValue("C.1.0", "C.1.0", "Hello!", "kWh", "10", "kWh")));

        when(mockPermission.connectionId()).thenReturn("connectionId");
        when(mockPermission.permissionId()).thenReturn("permissionId");
        when(mockPermission.dataNeed()).thenReturn(mockDataNeed);

        // When
        AiidaRecordStreamingDto dto = AiidaRecordConverter.recordToStreamingDto(aiidaRecord, mockPermission);

        // Then
        assertEquals("Hello!", dto.aiidaRecordValues().getFirst().rawValue());
        assertEquals("C.1.0", dto.aiidaRecordValues().getFirst().rawTag());
        assertEquals("connectionId", dto.connectionId());
        assertEquals("permissionId", dto.permissionId());
        assertEquals(timestamp.toEpochMilli(), dto.timestamp().toEpochMilli());
    }

    private static class DummyRecord extends AiidaRecord {
    }
}
