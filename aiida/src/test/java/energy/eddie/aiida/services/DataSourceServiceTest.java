package energy.eddie.aiida.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.adapters.datasource.modbus.ModbusDeviceTestHelper;
import energy.eddie.aiida.adapters.datasource.modbus.ModbusTcpDataSourceAdapter;
import energy.eddie.aiida.aggregator.Aggregator;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.config.datasource.it.SinapsiAlfaConfiguration;
import energy.eddie.aiida.dtos.datasource.DataSourceDto;
import energy.eddie.aiida.dtos.datasource.modbus.ModbusDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.at.OesterreichsEnergieDataSourceDto;
import energy.eddie.aiida.dtos.datasource.simulation.SimulationDataSourceDto;
import energy.eddie.aiida.dtos.events.DataSourceDeletionEvent;
import energy.eddie.aiida.errors.DataSourceNotFoundException;
import energy.eddie.aiida.errors.InvalidUserException;
import energy.eddie.aiida.errors.SinapsiAlflaEmptyConfigException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import energy.eddie.aiida.models.datasource.mqtt.at.OesterreichsEnergieDataSource;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.datasource.simulation.SimulationDataSource;
import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import energy.eddie.aiida.publisher.AiidaEventPublisher;
import energy.eddie.aiida.repositories.DataSourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSourceServiceTest {
    private static final DataSourceDto DATA_SOURCE_DTO = mock(OesterreichsEnergieDataSourceDto.class);
    private static final DataSource OUTBOUND_DATA_SOURCE = mock(SimulationDataSource.class);
    private static final MqttDataSource MQTT_OUTBOUND_DATA_SOURCE = mock(OesterreichsEnergieDataSource.class);
    private static final DataSource INBOUND_DATA_SOURCE = mock(InboundDataSource.class);
    private static final UUID DATA_SOURCE_ID = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID USER_ID = UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID VENDOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MODEL_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID DEVICE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private DataSourceRepository repository;
    @Mock
    private Aggregator aggregator;
    @Mock
    private AuthService authService;
    @Mock
    private MqttConfiguration mqttConfiguration;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Mock
    private SinapsiAlfaConfiguration sinapsiAlfaConfiguration;
    @Mock
    private AiidaEventPublisher aiidaEventPublisher;

    @InjectMocks
    private DataSourceService dataSourceService;

    @Test
    void shouldReturndataSourceByIdOrThrow() throws DataSourceNotFoundException {
        when(repository.findById(DATA_SOURCE_ID)).thenReturn(Optional.of(INBOUND_DATA_SOURCE));
        when(INBOUND_DATA_SOURCE.id()).thenReturn(DATA_SOURCE_ID);

        var result = dataSourceService.dataSourceByIdOrThrow(DATA_SOURCE_ID);

        assertEquals(DATA_SOURCE_ID, result.id());
        verify(repository, times(1)).findById(DATA_SOURCE_ID);
    }

    @Test
    void shouldReturnEmptyOptionalWhenDataSourceNotFound() {
        when(repository.findById(DATA_SOURCE_ID)).thenReturn(Optional.empty());

        assertThrows(DataSourceNotFoundException.class, () -> dataSourceService.dataSourceByIdOrThrow(DATA_SOURCE_ID));
    }

    @Test
    void shouldReturnOutboundDataSourceTypes() {
        var dataSourceTypes = dataSourceService.getOutboundDataSourceTypes();

        assertTrue(dataSourceTypes.containsAll(List.of(
                DataSourceType.SMART_GATEWAYS_ADAPTER,
                DataSourceType.SIMULATION,
                DataSourceType.MODBUS,
                DataSourceType.MICRO_TELEINFO,
                DataSourceType.SMART_METER_ADAPTER
        )));
        assertFalse(dataSourceTypes.contains(DataSourceType.INBOUND));
    }

    @Test
    void shouldReturnInboundDataSources() throws InvalidUserException {
        when(INBOUND_DATA_SOURCE.dataSourceType()).thenReturn(DataSourceType.INBOUND);
        when(OUTBOUND_DATA_SOURCE.dataSourceType()).thenReturn(DataSourceType.SIMULATION);
        when(repository.findByUserId(USER_ID)).thenReturn(List.of(INBOUND_DATA_SOURCE, OUTBOUND_DATA_SOURCE));
        when(authService.getCurrentUserId()).thenReturn(USER_ID);

        var result = dataSourceService.getInboundDataSources();

        assertTrue(result.contains(INBOUND_DATA_SOURCE));
        assertFalse(result.contains(OUTBOUND_DATA_SOURCE));
        verify(repository, times(1)).findByUserId(USER_ID);
    }

    @Test
    void shouldReturnOutboundDataSources() throws InvalidUserException {
        when(INBOUND_DATA_SOURCE.dataSourceType()).thenReturn(DataSourceType.INBOUND);
        when(OUTBOUND_DATA_SOURCE.dataSourceType()).thenReturn(DataSourceType.SIMULATION);
        when(repository.findByUserId(USER_ID)).thenReturn(List.of(INBOUND_DATA_SOURCE, OUTBOUND_DATA_SOURCE));
        when(authService.getCurrentUserId()).thenReturn(USER_ID);

        var result = dataSourceService.getOutboundDataSources();

        assertTrue(result.contains(OUTBOUND_DATA_SOURCE));
        assertFalse(result.contains(INBOUND_DATA_SOURCE));
        verify(repository, times(1)).findByUserId(USER_ID);
    }

    @Test
    void shouldAddNewDataSource() throws InvalidUserException, SinapsiAlflaEmptyConfigException {
        when(authService.getCurrentUserId()).thenReturn(USER_ID);
        when(mqttConfiguration.internalHost()).thenReturn("mqtt://test-broker");
        when(DATA_SOURCE_DTO.enabled()).thenReturn(true);

        var result = dataSourceService.addDataSource(DATA_SOURCE_DTO);

        assertNotNull(result.plaintextPassword());
        verify(bCryptPasswordEncoder, times(1)).encode(anyString());
        verify(repository, times(2)).save(any());
        verify(aggregator, times(1)).addNewDataSourceAdapter(any());
    }

    @Test
    void shouldAddModbusDataSource() throws InvalidUserException, SinapsiAlflaEmptyConfigException {
        try (
                MockedStatic<ModbusDeviceService> mockedStatic = mockStatic(ModbusDeviceService.class);
                MockedConstruction<ModbusTcpDataSourceAdapter> ignored = mockConstruction(ModbusTcpDataSourceAdapter.class)
        ) {
            mockedStatic.when(() -> ModbusDeviceService.loadConfig(any()))
                        .thenReturn(ModbusDeviceTestHelper.setupModbusDevice());
            when(authService.getCurrentUserId()).thenReturn(USER_ID);

            var dto = mock(ModbusDataSourceDto.class);
            when(dto.id()).thenReturn(DATA_SOURCE_ID);
            when(dto.dataSourceType()).thenReturn(DataSourceType.MODBUS);
            when(dto.modbusIp()).thenReturn("192.168.1.100");
            when(dto.modbusVendor()).thenReturn(VENDOR_ID);
            when(dto.modbusModel()).thenReturn(MODEL_ID);
            when(dto.modbusDevice()).thenReturn(DEVICE_ID);
            when(dto.enabled()).thenReturn(true);

            var result = dataSourceService.addDataSource(dto);

            assertNull(result.plaintextPassword());
            verify(repository, times(1)).save(any());
            verify(aggregator, times(1)).addNewDataSourceAdapter(any());
        }
    }

    @Test
    void shouldNotAddNewDataSource() throws InvalidUserException, SinapsiAlflaEmptyConfigException {
        when(authService.getCurrentUserId()).thenReturn(USER_ID);
        when(mqttConfiguration.internalHost()).thenReturn("mqtt://test-broker");
        when(DATA_SOURCE_DTO.enabled()).thenReturn(false);

        dataSourceService.addDataSource(DATA_SOURCE_DTO);

        verify(repository, times(2)).save(any());
        verify(aggregator, never()).addNewDataSourceAdapter(any());
    }

    @Test
    void shouldDeleteDataSource() {
        when(repository.findById(DATA_SOURCE_ID)).thenReturn(Optional.of(mock(DataSource.class)));
        dataSourceService.deleteDataSource(DATA_SOURCE_ID);

        verify(repository, times(1)).findById(DATA_SOURCE_ID);
        verify(aiidaEventPublisher, times(1)).publishEvent(any(DataSourceDeletionEvent.class));
        verify(repository, times(1)).delete(any(DataSource.class));
    }

    @Test
    void shouldUpdateDataSource() throws DataSourceNotFoundException {
        when(repository.findById(DATA_SOURCE_ID)).thenReturn(Optional.of(MQTT_OUTBOUND_DATA_SOURCE));

        when(DATA_SOURCE_DTO.id()).thenReturn(DATA_SOURCE_ID);
        dataSourceService.updateDataSource(DATA_SOURCE_DTO);

        verify(MQTT_OUTBOUND_DATA_SOURCE).update(DATA_SOURCE_DTO);
    }

    @Test
    void shouldAddDataSourcesOnStartDataSources() {
        UUID dataSourceId2 = UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606");

        when(OUTBOUND_DATA_SOURCE.enabled()).thenReturn(true);
        when(OUTBOUND_DATA_SOURCE.id()).thenReturn(UUID.randomUUID());
        when(MQTT_OUTBOUND_DATA_SOURCE.enabled()).thenReturn(false);
        when(MQTT_OUTBOUND_DATA_SOURCE.id()).thenReturn(dataSourceId2);
        when(INBOUND_DATA_SOURCE.enabled()).thenReturn(true);
        when(INBOUND_DATA_SOURCE.id()).thenReturn(UUID.randomUUID());

        when(repository.findAll()).thenReturn(List.of(OUTBOUND_DATA_SOURCE,
                                                      MQTT_OUTBOUND_DATA_SOURCE,
                                                      INBOUND_DATA_SOURCE));

        dataSourceService.startDataSources();

        verify(aggregator, times(2)).addNewDataSourceAdapter(any());
        verify(aggregator, never()).addNewDataSourceAdapter(argThat(ds -> ds.dataSource().id().equals(dataSourceId2)));
    }

    @Test
    void testEnableDataSource() throws DataSourceNotFoundException {
        var dto = mock(SimulationDataSourceDto.class);
        when(dto.id()).thenReturn(DATA_SOURCE_ID);
        var dataSource = new SimulationDataSource(dto, USER_ID);

        when(repository.findById(DATA_SOURCE_ID)).thenReturn(Optional.of(dataSource));

        dataSourceService.startDataSource(dataSource);

        dataSourceService.updateEnabledState(DATA_SOURCE_ID, true);
        dataSourceService.updateEnabledState(DATA_SOURCE_ID, true);
        dataSourceService.updateEnabledState(DATA_SOURCE_ID, false);
        dataSourceService.updateEnabledState(DATA_SOURCE_ID, false);

        verify(aggregator, times(2)).addNewDataSourceAdapter(any());
        verify(aggregator, times(2)).removeDataSourceAdapter(any());
    }

    @Test
    void shouldRegenerateSecrets() throws DataSourceNotFoundException {
        when(repository.findById(DATA_SOURCE_ID)).thenReturn(Optional.of(MQTT_OUTBOUND_DATA_SOURCE));
        when(MQTT_OUTBOUND_DATA_SOURCE.enabled()).thenReturn(true);

        var result = dataSourceService.regenerateSecrets(DATA_SOURCE_ID);

        assertNotNull(result.plaintextPassword());
        assertNotEquals(MQTT_OUTBOUND_DATA_SOURCE.password(), result.plaintextPassword());
        verify(bCryptPasswordEncoder, times(1)).encode(any());
        verify(aggregator, times(1)).addNewDataSourceAdapter(any());
    }

    @Test
    void shouldNotRegenerateSecretsIfNotMqtt() throws DataSourceNotFoundException {
        when(repository.findById(DATA_SOURCE_ID)).thenReturn(Optional.of(OUTBOUND_DATA_SOURCE));

        var result = dataSourceService.regenerateSecrets(DATA_SOURCE_ID);

        assertNull(result.plaintextPassword());
        verify(bCryptPasswordEncoder, never()).encode(any());
    }

    @Test
    void testCreateInboundDatasource() {
        // Given
        var permission = mock(Permission.class);
        var permissionId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var dataNeed = mock(AiidaLocalDataNeed.class);
        var mqttStreamingConfig = mock(MqttStreamingConfig.class);

        // When
        when(permission.id()).thenReturn(permissionId);
        when(permission.userId()).thenReturn(userId);
        when(permission.dataNeed()).thenReturn(dataNeed);
        when(permission.mqttStreamingConfig()).thenReturn(mqttStreamingConfig);
        dataSourceService.createInboundDataSource(permission);

        // Then
        verify(repository).save(any(InboundDataSource.class));
    }
}
