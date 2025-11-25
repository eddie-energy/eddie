package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.record.AiidaRecordValueDto;
import energy.eddie.aiida.dtos.record.LatestDataSourceRecordDto;
import energy.eddie.aiida.errors.datasource.DataSourceNotFoundException;
import energy.eddie.aiida.errors.datasource.InvalidDataSourceTypeException;
import energy.eddie.aiida.errors.permission.LatestPermissionRecordNotFoundException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.errors.record.InboundRecordNotFoundException;
import energy.eddie.aiida.errors.record.LatestAiidaRecordNotFoundException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.record.*;
import energy.eddie.aiida.repositories.AiidaRecordRepository;
import energy.eddie.aiida.repositories.DataSourceRepository;
import energy.eddie.aiida.utils.AiidaRecordConverter;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import energy.eddie.dataneeds.needs.aiida.AiidaSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

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
    private static final List<AiidaRecordValueDto> RECORD_VALUES = List.of(
            new AiidaRecordValueDto(
                    ObisCode.POSITIVE_ACTIVE_ENERGY.toString(),
                    ObisCode.POSITIVE_ACTIVE_ENERGY,
                    "25.5",
                    UnitOfMeasurement.WATT,
                    "25.5",
                    UnitOfMeasurement.WATT
            ),
            new AiidaRecordValueDto(
                    ObisCode.NEGATIVE_ACTIVE_ENERGY.toString(),
                    ObisCode.NEGATIVE_ACTIVE_ENERGY,
                    "60.0",
                    UnitOfMeasurement.WATT,
                    "60.0",
                    UnitOfMeasurement.WATT
            )
    );

    @Mock
    private Logger logger;
    @Mock
    private AiidaRecordRepository repository;
    @Mock
    private DataSourceRepository dataSourceRepository;
    @Mock
    private PermissionLatestRecordMap permissionLatestRecordMap;
    @Mock
    private InboundService inboundService;

    @InjectMocks
    private LatestRecordService aiidaRecordService;

    @Test
    void latestAiidaRecord_shouldReturnLatestRecord_whenFound() throws LatestAiidaRecordNotFoundException, DataSourceNotFoundException {
        var aiidaRecord = mock(AiidaRecord.class);
        var dataSource = mock(DataSource.class);
        when(aiidaRecord.timestamp()).thenReturn(TIMESTAMP);
        when(dataSource.id()).thenReturn(DATA_SOURCE_ID);
        when(dataSourceRepository.findById(DATA_SOURCE_ID))
                .thenReturn(Optional.of(dataSource));
        when(repository.findFirstByDataSourceIdOrderByIdDesc(DATA_SOURCE_ID))
                .thenReturn(Optional.of(aiidaRecord));

        var expectedDto = new LatestDataSourceRecordDto(
                TIMESTAMP,
                "datasource",
                AiidaAsset.SUBMETER,
                DATA_SOURCE_ID,
                RECORD_VALUES
        );

        try (MockedStatic<AiidaRecordConverter> mockedConverter = mockStatic(AiidaRecordConverter.class)) {
            mockedConverter.when(() -> AiidaRecordConverter.recordToLatestDto(aiidaRecord, dataSource))
                           .thenReturn(expectedDto);

            var result = aiidaRecordService.latestDataSourceRecord(DATA_SOURCE_ID);

            assertEquals(expectedDto, result);
            verify(dataSourceRepository, times(1)).findById(DATA_SOURCE_ID);
            verify(repository, times(1)).findFirstByDataSourceIdOrderByIdDesc(DATA_SOURCE_ID);
            mockedConverter.verify(() -> AiidaRecordConverter.recordToLatestDto(aiidaRecord, dataSource), times(1));
        }
    }

    @Test
    void latestAiidaRecord_shouldThrow_whenRecordNotFound() {
        var dataSource = mock(DataSource.class);
        when(dataSourceRepository.findById(DATA_SOURCE_ID))
                .thenReturn(Optional.of(dataSource));
        when(repository.findFirstByDataSourceIdOrderByIdDesc(DATA_SOURCE_ID))
                .thenReturn(Optional.empty());

        var exception = assertThrows(LatestAiidaRecordNotFoundException.class, () ->
                aiidaRecordService.latestDataSourceRecord(DATA_SOURCE_ID)
        );

        assertTrue(exception.getMessage().contains(DATA_SOURCE_ID.toString()));
        verify(dataSourceRepository, times(1)).findById(DATA_SOURCE_ID);
        verify(repository, times(1)).findFirstByDataSourceIdOrderByIdDesc(DATA_SOURCE_ID);
    }

    @Test
    void latestAiidaRecord_shouldThrow_whenDataSourceNotFound() {
        when(dataSourceRepository.findById(DATA_SOURCE_ID))
                .thenReturn(Optional.empty());

        var exception = assertThrows(DataSourceNotFoundException.class, () ->
                aiidaRecordService.latestDataSourceRecord(DATA_SOURCE_ID)
        );

        assertTrue(exception.getMessage().contains(DATA_SOURCE_ID.toString()));
        verify(dataSourceRepository, times(1)).findById(DATA_SOURCE_ID);
        verify(repository, never()).findFirstByDataSourceIdOrderByIdDesc(DATA_SOURCE_ID);
    }

    @Test
    void latestAiidaRecord_shouldLogTimestamp_whenRecordFound() throws LatestAiidaRecordNotFoundException, DataSourceNotFoundException {
        var aiidaRecord = mock(AiidaRecord.class);
        var dataSource = mock(DataSource.class);
        when(aiidaRecord.timestamp()).thenReturn(TIMESTAMP);
        when(dataSource.id()).thenReturn(DATA_SOURCE_ID);
        when(dataSourceRepository.findById(DATA_SOURCE_ID))
                .thenReturn(Optional.of(dataSource));
        when(repository.findFirstByDataSourceIdOrderByIdDesc(DATA_SOURCE_ID))
                .thenReturn(Optional.of(aiidaRecord));

        var expectedDto = new LatestDataSourceRecordDto(
                TIMESTAMP,
                "datasource",
                AiidaAsset.SUBMETER,
                DATA_SOURCE_ID,
                RECORD_VALUES
        );

        try (MockedStatic<AiidaRecordConverter> mockedConverter = mockStatic(AiidaRecordConverter.class)) {
            mockedConverter.when(() -> AiidaRecordConverter.recordToLatestDto(aiidaRecord, dataSource))
                           .thenReturn(expectedDto);

            aiidaRecordService.latestDataSourceRecord(DATA_SOURCE_ID);

            verify(aiidaRecord, times(1)).timestamp();
        }
    }

    @Test
    void latestAiidaRecord_shouldHandleDifferentDataSourceIds() throws LatestAiidaRecordNotFoundException, DataSourceNotFoundException {
        var differentDataSourceId = UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606");
        var aiidaRecord = mock(AiidaRecord.class);
        var dataSource = mock(DataSource.class);
        when(aiidaRecord.timestamp()).thenReturn(TIMESTAMP);
        when(dataSource.id()).thenReturn(differentDataSourceId);
        when(dataSourceRepository.findById(differentDataSourceId))
                .thenReturn(Optional.of(dataSource));
        when(repository.findFirstByDataSourceIdOrderByIdDesc(differentDataSourceId))
                .thenReturn(Optional.of(aiidaRecord));

        var expectedDto = new LatestDataSourceRecordDto(
                TIMESTAMP,
                "datasource",
                AiidaAsset.SUBMETER,
                differentDataSourceId,
                RECORD_VALUES
        );

        try (MockedStatic<AiidaRecordConverter> mockedConverter = mockStatic(AiidaRecordConverter.class)) {
            mockedConverter.when(() -> AiidaRecordConverter.recordToLatestDto(aiidaRecord, dataSource))
                           .thenReturn(expectedDto);

            var result = aiidaRecordService.latestDataSourceRecord(differentDataSourceId);

            assertEquals(expectedDto, result);
            verify(dataSourceRepository, times(1)).findById(differentDataSourceId);
            verify(repository, times(1)).findFirstByDataSourceIdOrderByIdDesc(differentDataSourceId);
        }
    }

    @Test
    void latestAiidaRecord_shouldPropagateConverterExceptions() {
        var aiidaRecord = mock(AiidaRecord.class);
        var dataSource = mock(DataSource.class);
        when(aiidaRecord.timestamp()).thenReturn(TIMESTAMP);
        when(dataSource.id()).thenReturn(DATA_SOURCE_ID);
        when(dataSourceRepository.findById(DATA_SOURCE_ID))
                .thenReturn(Optional.of(dataSource));
        when(repository.findFirstByDataSourceIdOrderByIdDesc(DATA_SOURCE_ID))
                .thenReturn(Optional.of(aiidaRecord));

        try (MockedStatic<AiidaRecordConverter> mockedConverter = mockStatic(AiidaRecordConverter.class)) {
            mockedConverter.when(() -> AiidaRecordConverter.recordToLatestDto(aiidaRecord, dataSource))
                           .thenThrow(new RuntimeException("Conversion failed"));

            assertThrows(RuntimeException.class, () ->
                    aiidaRecordService.latestDataSourceRecord(DATA_SOURCE_ID)
            );

            verify(dataSourceRepository, times(1)).findById(DATA_SOURCE_ID);
            verify(repository, times(1)).findFirstByDataSourceIdOrderByIdDesc(DATA_SOURCE_ID);
        }
    }

    @Test
    void latestOutboundPermissionRecord_shouldReturnLatestRecord_whenFound() throws LatestPermissionRecordNotFoundException {
        var permissionRecord = mock(PermissionLatestRecord.class);
        var latestSchema1 = new LatestRecordSchema(TIMESTAMP, "message1");
        var latestSchema2 = new LatestRecordSchema(TIMESTAMP.plusSeconds(10), "message2");
        var messages = new java.util.concurrent.ConcurrentHashMap<AiidaSchema, LatestRecordSchema>();
        messages.put(AiidaSchema.SMART_METER_P1_CIM, latestSchema1);
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
        when(inboundRecord.timestamp()).thenReturn(TIMESTAMP);
        when(inboundRecord.asset()).thenReturn(AiidaAsset.SUBMETER);
        when(inboundRecord.payload()).thenReturn(PAYLOAD);

        when(inboundService.latestRecord(PERMISSION_ID))
                .thenReturn(inboundRecord);

        var result = aiidaRecordService.latestInboundPermissionRecord(PERMISSION_ID);

        assertNotNull(result);
        assertEquals(TIMESTAMP, result.timestamp());
        assertEquals(AiidaAsset.SUBMETER, result.asset());
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