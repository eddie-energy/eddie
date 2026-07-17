// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.record;

import energy.eddie.aiida.errors.SecretLoadingException;
import energy.eddie.aiida.errors.auth.UnauthorizedException;
import energy.eddie.aiida.errors.datasource.InvalidDataSourceTypeException;
import energy.eddie.aiida.errors.permission.InvalidInboundPermissionException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.errors.record.InboundRecordNotFoundException;
import energy.eddie.aiida.errors.record.UnsupportedInboundRecordTransformationException;
import energy.eddie.aiida.models.datasource.interval.simulation.SimulationDataSource;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.permission.InboundMessageFormat;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.dataneed.InboundAiidaLocalDataNeed;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.repositories.InboundRecordRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.services.record.transform.InboundPayloadTransformationService;
import energy.eddie.aiida.services.secrets.SecretsService;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InboundRecordServiceTest {
    private static final UUID DATA_SOURCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final UUID PERMISSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final String ACCESS_CODE = "test-access-code";
    private static final Permission PERMISSION = new Permission();

    @Mock
    private InboundDataSource dataSource;

    @Mock
    private InboundRecordRepository inboundRecordRepository;
    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private InboundPayloadTransformationService inboundPayloadTransformationService;
    @Mock
    private SecretsService secretsService;

    @InjectMocks
    private InboundRecordService inboundRecordService;

    @BeforeEach
    void setUp() throws InvalidInboundPermissionException, SecretLoadingException {
        lenient().when(secretsService.loadSecret(ACCESS_CODE)).thenReturn(ACCESS_CODE);

        PERMISSION.setDataSource(dataSource);
        var dataNeed = mock(InboundAiidaLocalDataNeed.class);
        when(dataNeed.name()).thenReturn("someDataNeed");
        PERMISSION.setDataNeed(dataNeed);
        PERMISSION.updateInboundMessageFormat(InboundMessageFormat.CIM_1_12);
    }

    @Test
    void testLatestRecord_returnsMappedRecord() throws UnauthorizedException, PermissionNotFoundException,
                                                       InvalidDataSourceTypeException, InboundRecordNotFoundException,
                                                       UnsupportedInboundRecordTransformationException,
                                                       InvalidInboundPermissionException, SecretLoadingException {
        // Given
        var inboundRecord = inboundRecord();
        mockPermissionRepository();
        mockInboundRecordRepository(inboundRecord);
        when(inboundPayloadTransformationService.transform(inboundRecord, InboundMessageFormat.CIM_1_12))
                .thenReturn("mapped-payload");
        mockDataSource();

        // When
        var latestRecord = inboundRecordService.latestRecord(PERMISSION_ID, ACCESS_CODE);

        // Then
        assertEquals(DATA_SOURCE_ID, latestRecord.dataSourceId());
        assertEquals("mapped-payload", latestRecord.payload());
        assertEquals(AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12, latestRecord.schema());
        assertEquals(InboundMessageFormat.CIM_1_12, latestRecord.messageFormat());
    }

    @Test
    void testLatestRecord_withWrongDataSourceType_throwsException() {
        // Given
        var wrongPermission = new Permission();
        var wrongDataSource = mock(SimulationDataSource.class);
        wrongPermission.setDataSource(wrongDataSource);
        when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(wrongPermission));

        // When, Then
        assertThrows(InvalidDataSourceTypeException.class,
                     () -> inboundRecordService.latestRecord(PERMISSION_ID, ACCESS_CODE));
    }

    @Test
    void testLatestRecord_withWrongAccessCode_throwsException() {
        // Given
        mockPermissionRepository();

        // When, Then
        assertThrows(UnauthorizedException.class, () -> inboundRecordService.latestRecord(PERMISSION_ID, "wrong"));
    }

    @Test
    void testLatestRecord_withMissingDataSource_throwsException() {
        // Given
        when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.empty());

        // When, Then
        assertThrows(PermissionNotFoundException.class,
                     () -> inboundRecordService.latestRecord(PERMISSION_ID, ACCESS_CODE));
    }

    @Test
    void testLatestRecord_withMissingRecord_throwsException() {
        // Given
        mockDataSource();
        mockPermissionRepository();

        // When, Then
        assertThrows(InboundRecordNotFoundException.class,
                     () -> inboundRecordService.latestRecord(PERMISSION_ID, ACCESS_CODE));
    }

    @Test
    void testLatestRecord_withMissingInboundMessageFormat_throwsException() {
        var permissionWithoutInboundFormat = new Permission();
        permissionWithoutInboundFormat.setDataSource(dataSource);

        mockDataSource();
        when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(permissionWithoutInboundFormat));
        mockInboundRecordRepository(inboundRecord());

        assertThrows(InvalidInboundPermissionException.class,
                     () -> inboundRecordService.latestRecord(PERMISSION_ID, ACCESS_CODE));
    }

    private InboundRecord inboundRecord() {
        return new InboundRecord(
                Instant.now(),
                dataSource,
                AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12,
                "test"
        );
    }

    private void mockDataSource() {
        when(dataSource.id()).thenReturn(DATA_SOURCE_ID);
        when(dataSource.accessCode()).thenReturn(ACCESS_CODE);
    }

    private void mockPermissionRepository() {
        when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(PERMISSION));
    }

    private void mockInboundRecordRepository(InboundRecord inboundRecord) {
        when(inboundRecordRepository.findTopByDataSourceIdOrderByTimestampDesc(DATA_SOURCE_ID)).thenReturn(Optional.of(
                inboundRecord));
    }
}
