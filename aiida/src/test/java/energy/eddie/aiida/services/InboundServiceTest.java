package energy.eddie.aiida.services;

import energy.eddie.aiida.errors.SecretLoadingException;
import energy.eddie.aiida.errors.auth.UnauthorizedException;
import energy.eddie.aiida.errors.datasource.InvalidDataSourceTypeException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.errors.record.InboundRecordNotFoundException;
import energy.eddie.aiida.models.datasource.interval.simulation.SimulationDataSource;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.repositories.InboundRecordRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.services.secrets.SecretType;
import energy.eddie.aiida.services.secrets.SecretsService;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static energy.eddie.aiida.services.secrets.KeyStoreSecretsService.alias;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboundServiceTest {
    private static final UUID DATA_SOURCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PERMISSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final String ACCESS_CODE_ALIAS = alias(PERMISSION_ID, SecretType.API_KEY);
    private static final String ACCESS_CODE = "test-access-code";
    private static final Permission PERMISSION = new Permission();
    private static final InboundDataSource DATA_SOURCE = mock(InboundDataSource.class);
    private static final InboundRecord INBOUND_RECORD = new InboundRecord(
            Instant.now(),
            AiidaAsset.SUBMETER,
            USER_ID,
            DATA_SOURCE_ID,
            "test"
    );

    @Mock
    private InboundRecordRepository inboundRecordRepository;
    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private SecretsService secretsService;

    @InjectMocks
    private InboundService inboundService;

    @BeforeEach
    void setUp() {
        when(DATA_SOURCE.id()).thenReturn(DATA_SOURCE_ID);
        when(DATA_SOURCE.accessCode()).thenReturn(ACCESS_CODE_ALIAS);

        PERMISSION.setDataSource(DATA_SOURCE);
    }

    @Test
    void testLatestRecord_returnsRecord() throws UnauthorizedException, PermissionNotFoundException, InvalidDataSourceTypeException, InboundRecordNotFoundException, SecretLoadingException {
        // Given
        when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(PERMISSION));
        when(inboundRecordRepository.findTopByDataSourceIdOrderByTimestampDesc(DATA_SOURCE_ID)).thenReturn(Optional.of(
                INBOUND_RECORD));
        when(secretsService.loadSecret(ACCESS_CODE_ALIAS)).thenReturn(ACCESS_CODE);

        // When
        var inboundRecord = inboundService.latestRecord(PERMISSION_ID, ACCESS_CODE);

        // Then
        assertEquals(DATA_SOURCE_ID, inboundRecord.dataSourceId());
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
                     () -> inboundService.latestRecord(PERMISSION_ID, ACCESS_CODE));
    }

    @Test
    void testLatestRecord_withWrongAccessCode_throwsException() throws SecretLoadingException {
        // Given
        when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(PERMISSION));
        when(secretsService.loadSecret(ACCESS_CODE_ALIAS)).thenReturn(ACCESS_CODE);

        // When, Then
        assertThrows(UnauthorizedException.class, () -> inboundService.latestRecord(PERMISSION_ID, "wrong"));
    }

    @Test
    void testLatestRecord_withMissingDataSource_throwsException() {
        // Given
        when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.empty());

        // When, Then
        assertThrows(PermissionNotFoundException.class, () -> inboundService.latestRecord(PERMISSION_ID, ACCESS_CODE));
    }

    @Test
    void testLatestRecord_withMissingRecord_throwsException() throws SecretLoadingException {
        // Given
        when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(PERMISSION));
        when(secretsService.loadSecret(ACCESS_CODE_ALIAS)).thenReturn(ACCESS_CODE);

        // When, Then
        assertThrows(InboundRecordNotFoundException.class,
                     () -> inboundService.latestRecord(PERMISSION_ID, ACCESS_CODE));
    }
}
