// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.utils;

import energy.eddie.aiida.dtos.record.AiidaRecordStreamingDto;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.api.agnostic.aiida.AiidaAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static energy.eddie.api.agnostic.aiida.ObisCode.METER_SERIAL;
import static energy.eddie.api.agnostic.aiida.ObisCode.POSITIVE_ACTIVE_ENERGY;
import static energy.eddie.api.agnostic.aiida.UnitOfMeasurement.KILO_WATT;
import static energy.eddie.api.agnostic.aiida.UnitOfMeasurement.NONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiidaRecordConverterTest {
    private static final UUID dataSourceId = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID userId = UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606");
    private final UUID permissionId = UUID.fromString("41d0a13e-688a-454d-acab-7a6b2951cde2");
    @Mock
    private AiidaLocalDataNeed mockDataNeed;
    @Mock
    private Permission mockPermission;
    @Mock
    private DataSource mockDataSource;

    @Test
    void givenIntegerAiidaRecord_returnsDtoWithFieldsSet() {
        // Given
        Instant timestamp = Instant.now();
        var aiidaRecord = new AiidaRecord(timestamp, AiidaAsset.SUBMETER, userId, dataSourceId, List.of(
                new AiidaRecordValue("1-0:1.8.0", POSITIVE_ACTIVE_ENERGY, "23", KILO_WATT, "10", KILO_WATT)));
        when(mockPermission.connectionId()).thenReturn("connectionId");
        when(mockPermission.id()).thenReturn(permissionId);
        when(mockPermission.dataNeed()).thenReturn(mockDataNeed);

        // When
        AiidaRecordStreamingDto dto = AiidaRecordConverter.recordToStreamingDto(aiidaRecord, mockPermission);

        // Then
        assertEquals("10", dto.aiidaRecordValues().getFirst().value());
        assertEquals("23", dto.aiidaRecordValues().getFirst().rawValue());
        assertEquals("1-0:1.8.0", dto.aiidaRecordValues().getFirst().rawTag());
        assertEquals("connectionId", dto.connectionId());
        assertEquals(permissionId, dto.permissionId());
        assertEquals(dataSourceId, dto.dataSourceId());
        assertEquals(timestamp.toEpochMilli(), dto.timestamp().toEpochMilli());
    }

    @Test
    void givenStringAiidaRecord_returnsDtoWithFieldsSet() {
        // Given
        Instant timestamp = Instant.now();
        var aiidaRecord = new AiidaRecord(timestamp, AiidaAsset.SUBMETER, userId, dataSourceId, List.of(
                new AiidaRecordValue("0-0:C.1.0", METER_SERIAL, "Hello!", NONE, "10", NONE)));

        when(mockPermission.connectionId()).thenReturn("connectionId");
        when(mockPermission.id()).thenReturn(permissionId);
        when(mockPermission.dataNeed()).thenReturn(mockDataNeed);

        // When
        AiidaRecordStreamingDto dto = AiidaRecordConverter.recordToStreamingDto(aiidaRecord, mockPermission);

        // Then
        assertEquals("Hello!", dto.aiidaRecordValues().getFirst().rawValue());
        assertEquals("0-0:C.1.0", dto.aiidaRecordValues().getFirst().rawTag());
        assertEquals("connectionId", dto.connectionId());
        assertEquals(permissionId, dto.permissionId());
        assertEquals(dataSourceId, dto.dataSourceId());
        assertEquals(timestamp.toEpochMilli(), dto.timestamp().toEpochMilli());
    }

    @Test
    void givenAiidaRecordValue_whenConvertedToLatestDto_thenFieldsAreMappedCorrectly() {
        // Given
        Instant timestamp = Instant.now();
        var value = new AiidaRecordValue(
                "1-0:1.8.0",
                POSITIVE_ACTIVE_ENERGY,
                "23",
                KILO_WATT,
                "10",
                KILO_WATT
        );
        var aiidaRecord = new AiidaRecord(timestamp, AiidaAsset.SUBMETER, userId, dataSourceId, List.of(value));
        when(mockDataSource.name()).thenReturn("datasource");

        // When
        var dto = AiidaRecordConverter.recordToLatestDto(aiidaRecord, mockDataSource);
        var v = dto.aiidaRecordValues().getFirst();

        // Then
        assertEquals(POSITIVE_ACTIVE_ENERGY.toString(), v.rawTag());
        assertEquals(POSITIVE_ACTIVE_ENERGY, v.obisCode());
        assertEquals("23", v.rawValue());
        assertEquals(KILO_WATT, v.rawUnit());
        assertEquals("10", v.value());
        assertEquals(KILO_WATT, v.unit());
        assertEquals("datasource", dto.name());
    }
}
