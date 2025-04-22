package energy.eddie.aiida.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.adapters.datasource.modbus.ModbusDeviceTestHelper;
import energy.eddie.aiida.adapters.datasource.modbus.ModbusTcpDataSourceAdapter;
import energy.eddie.aiida.adapters.datasource.DataSourceAdapter;
import energy.eddie.aiida.aggregator.Aggregator;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.adapters.datasource.DataSourceAdapter;
import energy.eddie.aiida.dtos.DataSourceModbusDto;
import energy.eddie.aiida.models.datasource.at.OesterreichsEnergieDataSource;
import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceMqttDto;
import energy.eddie.aiida.errors.InvalidUserException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.at.OesterreichsEnergieDataSource;
import energy.eddie.aiida.repositories.DataSourceRepository;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataSourceServiceTest {
    private static final UUID DATA_SOURCE_ID = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");

    private static final UUID VENDOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MODEL_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID DEVICE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private DataSourceRepository repository;
    private Aggregator aggregator;
    private AuthService authService;
    private MqttConfiguration mqttConfiguration;
    private ObjectMapper objectMapper;
    private DataSourceService dataSourceService;
    private UUID userId;

    DataSourceDto createNewDataSourceDto(UUID id, DataSourceType type, String name, boolean enabled) {
        return new DataSourceDto(id, type, AiidaAsset.SUBMETER, name, enabled, "", 1, null, null);
    }

    DataSource createNewDataSource(UUID id, DataSourceType type) {
        return DataSource.createFromDto(
                createNewDataSourceDto(id, type, "Test", true),
                userId,
                new DataSourceMqttDto("tcp://localhost:1883","tcp://localhost:1883","aiida/test", "user", "pw")
        );
    }

    @BeforeEach
    void setUp() {
        repository = mock(DataSourceRepository.class);
        aggregator = mock(Aggregator.class);
        authService = mock(AuthService.class);
        mqttConfiguration = mock(MqttConfiguration.class);
        objectMapper = mock(ObjectMapper.class);
        userId = UUID.randomUUID();
        dataSourceService = spy(new DataSourceService(repository,
                                                      aggregator,
                                                      authService,
                                                      mqttConfiguration,
                                                      objectMapper));
    }

    @Test
    void shouldReturnDataSourceById() {
        var dataSource = createNewDataSource(DATA_SOURCE_ID, DataSourceType.SMART_GATEWAYS_ADAPTER);
        when(repository.findById(DATA_SOURCE_ID)).thenReturn(Optional.of(dataSource));

        var result = dataSourceService.getDataSourceById(DATA_SOURCE_ID);

        assertTrue(result.isPresent());
        assertEquals(DATA_SOURCE_ID, result.get().id());
        verify(repository, times(1)).findById(DATA_SOURCE_ID);
    }

    @Test
    void shouldReturnEmptyOptionalWhenDataSourceNotFound() {
        when(repository.findById(DATA_SOURCE_ID)).thenReturn(Optional.empty());

        var result = dataSourceService.getDataSourceById(DATA_SOURCE_ID);

        assertTrue(result.isEmpty(), "Expected Optional to be empty when DataSource is not found.");
    }

    @Test
    void shouldAddNewDataSource() throws InvalidUserException {
        when(authService.getCurrentUserId()).thenReturn(userId);
        when(mqttConfiguration.internalHost()).thenReturn("mqtt://test-broker");

        dataSourceService.addDataSource(createNewDataSourceDto(DATA_SOURCE_ID,
                                                               DataSourceType.SMART_GATEWAYS_ADAPTER,
                                                               "Test",
                                                               true));

        verify(repository, times(2)).save(any());
        verify(aggregator, times(1)).addNewDataSourceAdapter(any());
    }

    @Test
    void shouldAddModbusDataSource() throws InvalidUserException {
        try (
                MockedStatic<ModbusDeviceService> mockedStatic = mockStatic(ModbusDeviceService.class);
                MockedConstruction<ModbusTcpDataSourceAdapter> ignored = mockConstruction(ModbusTcpDataSourceAdapter.class)
        ) {
            mockedStatic.when(() -> ModbusDeviceService.loadConfig(any()))
                    .thenReturn(ModbusDeviceTestHelper.setupModbusDevice());
            when(authService.getCurrentUserId()).thenReturn(userId);

            var modbusSettings = new DataSourceModbusDto(
                    "192.168.1.100",
                    VENDOR_ID,
                    MODEL_ID,
                    DEVICE_ID
            );

            var dto = new DataSourceDto(
                    DATA_SOURCE_ID,
                    DataSourceType.MODBUS.identifier(),
                    AiidaAsset.SUBMETER.asset(),
                    "Modbus DS",
                    true,
                    "",
                    1,
                    null,
                    modbusSettings
            );

            dataSourceService.addDataSource(dto);

            verify(repository, times(1)).save(any());
            verify(aggregator, times(1)).addNewDataSourceAdapter(any());
        }
    }

    @Test
    void shouldNotAddNewDataSource() throws InvalidUserException {
        when(authService.getCurrentUserId()).thenReturn(userId);
        when(mqttConfiguration.internalHost()).thenReturn("mqtt://test-broker");

        dataSourceService.addDataSource(createNewDataSourceDto(DATA_SOURCE_ID,
                                                               DataSourceType.SMART_GATEWAYS_ADAPTER,
                                                               "Test",
                                                               false));

        verify(repository, times(2)).save(any());
        verify(aggregator, never()).addNewDataSourceAdapter(any());
    }

    @Test
    void shouldDeleteDataSource() {
        doNothing().when(aggregator).addNewDataSourceAdapter(any());
        dataSourceService.deleteDataSource(DATA_SOURCE_ID);

        verify(repository, times(1)).deleteById(DATA_SOURCE_ID);
    }

    @Test
    void shouldUpdateDataSource() throws InvalidUserException {
        var dataSource = createNewDataSource(DATA_SOURCE_ID, DataSourceType.SMART_METER_ADAPTER);

        when(repository.findById(DATA_SOURCE_ID)).thenReturn(Optional.of(dataSource));
        when(repository.save(any(OesterreichsEnergieDataSource.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var updatedDataSourceDto = createNewDataSourceDto(DATA_SOURCE_ID,
                                                          DataSourceType.SMART_METER_ADAPTER,
                                                          "New Name",
                                                          false);

        var savedDataSource = (OesterreichsEnergieDataSource) dataSourceService.updateDataSource(updatedDataSourceDto);

        assertEquals("New Name", savedDataSource.name());
        assertFalse(savedDataSource.enabled());
    }

    @Test
    void shouldAddDataSourcesOnStartDataSources() {
        UUID dataSourceId1 = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
        UUID dataSourceId2 = UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606");
        UUID dataSourceId3 = UUID.fromString("6211ea05-d4ab-48ff-8613-8f4791a56606");

        var oesterreichsEnergieDataSource = createNewDataSource(dataSourceId1, DataSourceType.SMART_METER_ADAPTER);
        var microTeleinfoV3DataSource = createNewDataSource(dataSourceId2, DataSourceType.MICRO_TELEINFO);
        var simulationDataSource = createNewDataSource(dataSourceId3, DataSourceType.SIMULATION);

        oesterreichsEnergieDataSource.setEnabled(true);
        microTeleinfoV3DataSource.setEnabled(false);
        simulationDataSource.setEnabled(true);

        when(repository.findAll()).thenReturn(List.of(oesterreichsEnergieDataSource,
                                                      microTeleinfoV3DataSource,
                                                      simulationDataSource));

        var service = new DataSourceService(repository, aggregator, authService, mqttConfiguration, objectMapper);
        service.startDataSources();

        verify(aggregator, times(2)).addNewDataSourceAdapter(any());
        verify(aggregator, never()).addNewDataSourceAdapter(argThat(ds -> ds.dataSource().id().equals(dataSourceId2)));
    }

    @Test
    void testEnableDataSource() {
        var dataSource = createNewDataSource(DATA_SOURCE_ID, DataSourceType.MICRO_TELEINFO);
        dataSource.setEnabled(false);
        var mockAdapter = mock(DataSourceAdapter.class);

        when(repository.findById(DATA_SOURCE_ID)).thenReturn(Optional.of(dataSource));
        doReturn(Optional.of(mockAdapter)).when(dataSourceService).findDataSourceAdapter(DATA_SOURCE_ID);

        dataSourceService.updateEnabledState(DATA_SOURCE_ID, true);

        verify(repository, times(1)).save(argThat(DataSource::enabled));
        verify(aggregator, times(1)).addNewDataSourceAdapter(any());

        dataSourceService.updateEnabledState(DATA_SOURCE_ID, false);

        verify(repository, times(2)).save(argThat(ds -> !ds.enabled()));
        verify(aggregator, times(1)).removeDataSourceAdapter(any());
    }
}
