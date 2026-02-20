// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.errors.datasource.InvalidDataSourceTypeException;
import energy.eddie.aiida.errors.permission.LatestPermissionRecordNotFoundException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.errors.record.InboundRecordNotFoundException;
import energy.eddie.aiida.errors.record.LatestAiidaRecordNotFoundException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.record.*;
import energy.eddie.aiida.repositories.AiidaRecordRepository;
import energy.eddie.api.agnostic.aiida.AiidaAsset;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LatestRecordServiceTest {
    private static final UUID DATA_SOURCE_ID = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID PERMISSION_ID = UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final String TOPIC = "test/topic";
    private static final String SERVER_URI = "mqtt://test.server.com";
    private static final String PAYLOAD = "test-payload-data";
    private static final Instant TIMESTAMP = Instant.parse("2024-01-15T10:30:00Z");

    @Mock
    private AiidaRecordRepository repository;
    @Mock
    private PermissionLatestRecordMap permissionLatestRecordMap;
    @Mock
    private InboundService inboundService;

    @InjectMocks
    private LatestRecordService aiidaRecordService;

    @Test
    void latestAiidaRecord_shouldReturnLatestRecord_whenFound() throws LatestAiidaRecordNotFoundException {
        // Given
        var aiidaRecord = mock(AiidaRecord.class);
        var dataSource = mock(DataSource.class);

        var value = new AiidaRecordValue(
                "my-raw-tag",
                ObisCode.POSITIVE_ACTIVE_ENERGY,
                "0.0255",
                UnitOfMeasurement.KILO_WATT,
                "25.5",
                UnitOfMeasurement.WATT
        );

        when(aiidaRecord.timestamp()).thenReturn(TIMESTAMP);
        when(aiidaRecord.dataSource()).thenReturn(dataSource);
        when(aiidaRecord.aiidaRecordValues()).thenReturn(List.of(value));

        when(dataSource.id()).thenReturn(DATA_SOURCE_ID);
        when(dataSource.asset()).thenReturn(AiidaAsset.SUBMETER);
        when(dataSource.name()).thenReturn("datasource");

        when(repository.findFirstByDataSourceIdOrderByIdDesc(DATA_SOURCE_ID))
                .thenReturn(Optional.of(aiidaRecord));

        // When
        var result = aiidaRecordService.latestDataSourceRecord(DATA_SOURCE_ID);

        // Then
        assertEquals(TIMESTAMP, result.timestamp());
        assertEquals("datasource", result.name());
        assertEquals(AiidaAsset.SUBMETER, result.asset());
        assertEquals(DATA_SOURCE_ID, result.dataSourceId());
        assertEquals(value.toDto(), result.aiidaRecordValues().getFirst());
    }

    @Test
    void latestAiidaRecord_shouldThrow_whenRecordNotFound() {
        when(repository.findFirstByDataSourceIdOrderByIdDesc(DATA_SOURCE_ID))
                .thenReturn(Optional.empty());

        var exception = assertThrows(LatestAiidaRecordNotFoundException.class, () ->
                aiidaRecordService.latestDataSourceRecord(DATA_SOURCE_ID)
        );

        assertTrue(exception.getMessage().contains(DATA_SOURCE_ID.toString()));
        verify(repository, times(1)).findFirstByDataSourceIdOrderByIdDesc(DATA_SOURCE_ID);
    }

    @Test
    void latestAiidaRecords_shouldReturnLatestRecords_whenFound() throws LatestAiidaRecordNotFoundException, DataSourceNotFoundException {
        int nRecords = 5;

        var timestamps = new Instant[] {
                TIMESTAMP,
                Instant.parse("2026-01-15T10:29:00Z"),
                Instant.parse("2026-01-15T10:28:00Z"),
                Instant.parse("2026-01-15T10:27:00Z"),
                Instant.parse("2026-01-15T10:26:00Z")
        };

        var aiidaRecords = new AiidaRecord[nRecords];
        var expectedDtos = new LatestDataSourceRecordDto[nRecords];
        var dataSource = mock(DataSource.class);

        for (int i = 0; i < nRecords; i++) {
            var aiidaRecord = mock(AiidaRecord.class);
            when(dataSourceRepository.findById(DATA_SOURCE_ID))
                    .thenReturn(Optional.of(dataSource));

            aiidaRecords[i] = aiidaRecord;
            var expectedDto = new LatestDataSourceRecordDto(timestamps[i],
                    "datasource%d".formatted(i + 1),
                    AiidaAsset.SUBMETER,
                    DATA_SOURCE_ID,
                    RECORD_VALUES);
            expectedDtos[i] = expectedDto;
        }
        
        when(repository.findByDataSourceIdOrderByTimestampDesc(DATA_SOURCE_ID, Pageable.ofSize(nRecords)))
                .thenReturn(List.of(aiidaRecords));

        try (MockedStatic<AiidaRecordConverter> mockedConverter = mockStatic(AiidaRecordConverter.class)) {
            for (int i = 0; i < nRecords; i++) {
                int finalI = i;
                mockedConverter.when(() -> AiidaRecordConverter.recordToLatestDto(aiidaRecords[finalI], dataSource))
                        .thenReturn(expectedDtos[i]);
            }

            var resultList = aiidaRecordService.latestDataSourceRecords(DATA_SOURCE_ID, nRecords);

            for (int i = 0; i < nRecords; i++) {
                assertEquals(expectedDtos[i], resultList.get(i));
            }

            verify(dataSourceRepository, times(1)).findById(DATA_SOURCE_ID);
            verify(repository, times(1)).findByDataSourceIdOrderByTimestampDesc(DATA_SOURCE_ID, Pageable.ofSize(nRecords));
            for (int i = 0; i < nRecords; i++) {
                int finalI = i;
                mockedConverter.verify(() -> AiidaRecordConverter.recordToLatestDto(aiidaRecords[finalI], dataSource), times(1));
            }
        }
    }

    @Test
    void latestAiidaRecords_shouldThrow_whenRecordNotFound() {
        var dataSource = mock(DataSource.class);
        when(dataSourceRepository.findById(DATA_SOURCE_ID))
                .thenReturn(Optional.of(dataSource));
        when(repository.findByDataSourceIdOrderByTimestampDesc(DATA_SOURCE_ID, Pageable.ofSize(3)))
                .thenReturn(List.of());

        var exception = assertThrows(LatestAiidaRecordNotFoundException.class, () ->
                aiidaRecordService.latestDataSourceRecords(DATA_SOURCE_ID, 3)
        );

        assertTrue(exception.getMessage().contains(DATA_SOURCE_ID.toString()));
        verify(dataSourceRepository, times(1)).findById(DATA_SOURCE_ID);
        verify(repository, times(1)).findByDataSourceIdOrderByTimestampDesc(DATA_SOURCE_ID, Pageable.ofSize(3));
    }

    @Test
    void latestAiidaRecords_shouldThrow_whenDataSourceNotFound() {
        when(dataSourceRepository.findById(DATA_SOURCE_ID))
                .thenReturn(Optional.empty());

        var exception = assertThrows(DataSourceNotFoundException.class, () ->
                aiidaRecordService.latestDataSourceRecords(DATA_SOURCE_ID, 3)
        );

        assertTrue(exception.getMessage().contains(DATA_SOURCE_ID.toString()));
        verify(dataSourceRepository, times(1)).findById(DATA_SOURCE_ID);
        verify(repository, never()).findByDataSourceIdOrderByTimestampDesc(DATA_SOURCE_ID, Pageable.ofSize(3));
    }

    @Test
    void latestOutboundPermissionRecord_shouldReturnLatestRecord_whenFound() throws LatestPermissionRecordNotFoundException {
        var permissionRecord = mock(PermissionLatestRecord.class);
        var latestSchema1 = new LatestRecordSchema(TIMESTAMP, "message1");
        var latestSchema2 = new LatestRecordSchema(TIMESTAMP.plusSeconds(10), "message2");
        var messages = new java.util.concurrent.ConcurrentHashMap<AiidaSchema, LatestRecordSchema>();
        messages.put(AiidaSchema.SMART_METER_P1_CIM_V1_04, latestSchema1);
        messages.put(AiidaSchema.SMART_METER_P1_RAW, latestSchema2);

        when(permissionRecord.topic()).thenReturn(TOPIC);
        when(permissionRecord.serverUri()).thenReturn(SERVER_URI);
        when(permissionRecord.messages()).thenReturn(messages);
        when(permissionLatestRecordMap.get(PERMISSION_ID))
                .thenReturn(Optional.of(permissionRecord));

        var result = aiidaRecordService.latestOutboundPermissionRecord(PERMISSION_ID);

        assertNotNull(result);
        assertEquals(PERMISSION_ID, result.permissionId());
        assertEquals(TOPIC, result.topic());
        assertEquals(SERVER_URI, result.serverUri());
        assertEquals(2, result.messages().size());

        verify(permissionLatestRecordMap, times(1)).get(PERMISSION_ID);
    }

    @Test
    void latestOutboundPermissionRecord_shouldThrow_whenPermissionNotFound() {
        when(permissionLatestRecordMap.get(PERMISSION_ID))
                .thenReturn(Optional.empty());

        var exception = assertThrows(LatestPermissionRecordNotFoundException.class, () ->
                aiidaRecordService.latestOutboundPermissionRecord(PERMISSION_ID)
        );

        assertTrue(exception.getMessage().contains(PERMISSION_ID.toString()));
        verify(permissionLatestRecordMap, times(1)).get(PERMISSION_ID);
    }

    @Test
    void latestOutboundPermissionRecord_shouldHandleEmptyMessages() throws LatestPermissionRecordNotFoundException {
        var permissionRecord = mock(PermissionLatestRecord.class);
        var emptyMessages = new java.util.concurrent.ConcurrentHashMap<AiidaSchema, LatestRecordSchema>();

        when(permissionRecord.topic()).thenReturn(TOPIC);
        when(permissionRecord.serverUri()).thenReturn(SERVER_URI);
        when(permissionRecord.messages()).thenReturn(emptyMessages);
        when(permissionLatestRecordMap.get(PERMISSION_ID))
                .thenReturn(Optional.of(permissionRecord));

        var result = aiidaRecordService.latestOutboundPermissionRecord(PERMISSION_ID);

        assertNotNull(result);
        assertEquals(PERMISSION_ID, result.permissionId());
        assertEquals(TOPIC, result.topic());
        assertEquals(SERVER_URI, result.serverUri());
        assertEquals(0, result.messages().size());

        verify(permissionLatestRecordMap, times(1)).get(PERMISSION_ID);
    }

    @Test
    void latestInboundPermissionRecord_shouldReturnLatestRecord_whenFound()
            throws PermissionNotFoundException, InvalidDataSourceTypeException, InboundRecordNotFoundException {
        var inboundRecord = mock(InboundRecord.class);
        var dataSource = mock(DataSource.class);
        when(inboundRecord.timestamp()).thenReturn(TIMESTAMP);
        when(inboundRecord.dataSource()).thenReturn(dataSource);
        when(dataSource.id()).thenReturn(DATA_SOURCE_ID);
        when(inboundRecord.payload()).thenReturn(PAYLOAD);

        when(inboundService.latestRecord(PERMISSION_ID))
                .thenReturn(inboundRecord);

        var result = aiidaRecordService.latestInboundPermissionRecord(PERMISSION_ID);

        assertNotNull(result);
        assertEquals(TIMESTAMP, result.timestamp());
        assertEquals(DATA_SOURCE_ID, result.dataSourceId());
        assertEquals(PAYLOAD, result.payload());

        verify(inboundService, times(1)).latestRecord(PERMISSION_ID);
    }

    @Test
    void latestInboundPermissionRecord_shouldPropagatePermissionNotFoundException()
            throws PermissionNotFoundException, InvalidDataSourceTypeException, InboundRecordNotFoundException {
        when(inboundService.latestRecord(PERMISSION_ID))
                .thenThrow(new PermissionNotFoundException(PERMISSION_ID));

        assertThrows(PermissionNotFoundException.class, () ->
                aiidaRecordService.latestInboundPermissionRecord(PERMISSION_ID)
        );

        verify(inboundService, times(1)).latestRecord(PERMISSION_ID);
    }

    @Test
    void latestInboundPermissionRecord_shouldPropagateInboundRecordNotFoundException()
            throws PermissionNotFoundException, InvalidDataSourceTypeException, InboundRecordNotFoundException {
        when(inboundService.latestRecord(PERMISSION_ID))
                .thenThrow(new InboundRecordNotFoundException(UUID.randomUUID()));

        assertThrows(InboundRecordNotFoundException.class, () ->
                aiidaRecordService.latestInboundPermissionRecord(PERMISSION_ID)
        );

        verify(inboundService, times(1)).latestRecord(PERMISSION_ID);
    }
}