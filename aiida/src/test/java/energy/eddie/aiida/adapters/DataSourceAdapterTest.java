package energy.eddie.aiida.adapters;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.adapters.datasource.DataSourceAdapter;
import energy.eddie.aiida.adapters.datasource.at.OesterreichsEnergieAdapter;
import energy.eddie.aiida.adapters.datasource.fr.MicroTeleinfoV3Adapter;
import energy.eddie.aiida.adapters.datasource.modbus.ModbusDeviceTestHelper;
import energy.eddie.aiida.adapters.datasource.modbus.ModbusTcpClient;
import energy.eddie.aiida.adapters.datasource.modbus.ModbusTcpDataSourceAdapter;
import energy.eddie.aiida.adapters.datasource.sga.SmartGatewaysAdapter;
import energy.eddie.aiida.adapters.datasource.simulation.SimulationAdapter;
import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceModbusDto;
import energy.eddie.aiida.dtos.DataSourceMqttDto;
import energy.eddie.aiida.dtos.DataSourceProtocolSettings;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.services.ModbusDeviceService;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;

class DataSourceAdapterTest {
    private static final UUID VENDOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MODEL_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID DEVICE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ID = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
    private ObjectMapper mapper;

    DataSource createNewDataSource(DataSourceType type) {
        DataSourceProtocolSettings settings;

        if (type == DataSourceType.MODBUS) {
            settings = new DataSourceModbusDto("127.0.0.1", VENDOR_ID, MODEL_ID, DEVICE_ID);
        } else {
            settings = new DataSourceMqttDto("tcp://localhost:1883","tcp://localhost:1883", "aiida/test", "user", "pw");
        }

        return DataSource.createFromDto(
                new DataSourceDto(ID, type, AiidaAsset.SUBMETER, "test", "AT", true, 1, null, null),
                ID,
                settings
        );
    }


    @BeforeEach
    void setUp() {
        mapper = new AiidaConfiguration().customObjectMapper().build();
    }

    @Test
    void givenOesterreichsEnergie_returnsAdapter() {
        // Given
        var dataSource = createNewDataSource(DataSourceType.SMART_METER_ADAPTER);

        // When
        var adapter = DataSourceAdapter.create(dataSource, mapper);

        // Then
        assertInstanceOf(OesterreichsEnergieAdapter.class, adapter);
    }

    @Test
    void givenMicroTeleinfo_returnsAdapter() {
        // Given
        var dataSource = createNewDataSource(DataSourceType.MICRO_TELEINFO);

        // When
        var adapter = DataSourceAdapter.create(dataSource, mapper);

        // Then
        assertInstanceOf(MicroTeleinfoV3Adapter.class, adapter);
    }

    @Test
    void givenSmartGateways_returnsAdapter() {
        // Given
        var dataSource = createNewDataSource(DataSourceType.SMART_GATEWAYS_ADAPTER);

        // When
        var adapter = DataSourceAdapter.create(dataSource, mapper);

        // Then
        assertInstanceOf(SmartGatewaysAdapter.class, adapter);
    }

    @Test
    void givenSimulation_returnsAdapter() {
        // Given
        var dataSource = createNewDataSource(DataSourceType.SIMULATION);

        // When
        var adapter = DataSourceAdapter.create(dataSource, mapper);

        // Then
        assertInstanceOf(SimulationAdapter.class, adapter);
    }

    @Test
    @ExtendWith(MockitoExtension.class)
    void givenModbus_returnsAdapter() {
        try (MockedStatic<ModbusDeviceService> mockedStatic = mockStatic(ModbusDeviceService.class);
             MockedConstruction<ModbusTcpClient> mockedClient = mockConstruction(ModbusTcpClient.class)) {

            mockedStatic.when(() -> ModbusDeviceService.loadConfig(any()))
                    .thenReturn(ModbusDeviceTestHelper.setupModbusDevice());

            var dataSource = createNewDataSource(DataSourceType.MODBUS);

            var adapter = DataSourceAdapter.create(dataSource, mapper);

            assertInstanceOf(ModbusTcpDataSourceAdapter.class, adapter);
        }
    }
}
