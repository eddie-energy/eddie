package energy.eddie.aiida.utils;

import energy.eddie.aiida.dtos.AiidaRecordStreamingDto;
import energy.eddie.aiida.models.permission.AiidaLocalDataNeed;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

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
    void givenNotImplementedInheritor_throws() {
        // Given
        when(mockPermission.connectionId()).thenReturn("connectionId");
        when(mockPermission.dataNeed()).thenReturn(mockDataNeed);
        var dummyRecord = new DummyRecord();

        // When, Then
        assertThrows(IllegalArgumentException.class,
                     () -> AiidaRecordConverter.recordToStreamingDto(dummyRecord, mockPermission));
    }

    @Test
    void givenIntegerAiidaRecord_returnsDtoWithFieldsSet() {
        // Given
        Instant timestamp = Instant.now();
        var aiidaRecord = AiidaRecordFactory.createRecord("1.8.0", timestamp, 23);
        when(mockPermission.connectionId()).thenReturn("connectionId");
        when(mockPermission.permissionId()).thenReturn("permissionId");
        when(mockPermission.dataNeed()).thenReturn(mockDataNeed);

        // When
        AiidaRecordStreamingDto dto = AiidaRecordConverter.recordToStreamingDto(aiidaRecord, mockPermission);

        // Then
        assertEquals(23, dto.value());
        assertEquals("1.8.0", dto.code());
        assertEquals("connectionId", dto.connectionId());
        assertEquals("permissionId", dto.permissionId());
        assertEquals(timestamp.toEpochMilli(), dto.timestamp().toEpochMilli());
    }

    @Test
    void givenStringAiidaRecord_returnsDtoWithFieldsSet() {
        // Given
        Instant timestamp = Instant.now();
        var aiidaRecord = AiidaRecordFactory.createRecord("C.1.0", timestamp, "Hello!");
        when(mockPermission.connectionId()).thenReturn("connectionId");
        when(mockPermission.permissionId()).thenReturn("permissionId");
        when(mockPermission.dataNeed()).thenReturn(mockDataNeed);

        // When
        AiidaRecordStreamingDto dto = AiidaRecordConverter.recordToStreamingDto(aiidaRecord, mockPermission);

        // Then
        assertEquals("Hello!", dto.value());
        assertEquals("C.1.0", dto.code());
        assertEquals("connectionId", dto.connectionId());
        assertEquals("permissionId", dto.permissionId());
        assertEquals(timestamp.toEpochMilli(), dto.timestamp().toEpochMilli());
    }

    private static class DummyRecord extends AiidaRecord {}
}
