package energy.eddie.aiida.utils;

import energy.eddie.aiida.dtos.AiidaRecordStreamingDto;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordFactory;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiidaRecordConverterTest {
    @Test
    void givenNotImplementedInheritor_throws() {
        var dummyRecord = new DummyRecord();
        assertThrows(IllegalArgumentException.class,
                () -> AiidaRecordConverter.recordToStreamingDto(dummyRecord, mock(Permission.class)));
    }

    @Test
    void givenIntegerAiidaRecord_returnsDtoWithFieldsSet() {
        // Given
        var record = AiidaRecordFactory.createRecord("1.8.0", Instant.now(), 23);
        var permission = mock(Permission.class);
        when(permission.connectionId()).thenReturn("connectionId");
        when(permission.dataNeedId()).thenReturn("dataNeedId");

        // When
        AiidaRecordStreamingDto dto = AiidaRecordConverter.recordToStreamingDto(record, permission);

        // Then
        assertEquals(23, dto.value());
        assertEquals("1.8.0", dto.code());
        assertEquals("connectionId", dto.connectionId());
        assertEquals("dataNeedId", dto.dataNeedId());
    }

    @Test
    void givenStringAiidaRecord_returnsDtoWithFieldsSet() {
        // Given
        var record = AiidaRecordFactory.createRecord("C.1.0", Instant.now(), "Hello!");
        var permission = mock(Permission.class);
        when(permission.connectionId()).thenReturn("connectionId");
        when(permission.dataNeedId()).thenReturn("dataNeedId");

        // When
        AiidaRecordStreamingDto dto = AiidaRecordConverter.recordToStreamingDto(record, permission);

        // Then
        assertEquals("Hello!", dto.value());
        assertEquals("C.1.0", dto.code());
        assertEquals("connectionId", dto.connectionId());
        assertEquals("dataNeedId", dto.dataNeedId());
    }

    private static class DummyRecord extends AiidaRecord {
    }
}
