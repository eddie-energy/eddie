package energy.eddie.aiida.adapters;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.adapters.datasource.DataSourceAdapter;
import energy.eddie.aiida.adapters.datasource.at.OesterreichsEnergieAdapter;
import energy.eddie.aiida.adapters.datasource.fr.MicroTeleinfoV3Adapter;
import energy.eddie.aiida.adapters.datasource.inbound.InboundAdapter;
import energy.eddie.aiida.adapters.datasource.it.SinapsiAlfaAdapter;
import energy.eddie.aiida.adapters.datasource.modbus.ModbusDeviceTestHelper;
import energy.eddie.aiida.adapters.datasource.modbus.ModbusTcpClient;
import energy.eddie.aiida.adapters.datasource.modbus.ModbusTcpDataSourceAdapter;
import energy.eddie.aiida.adapters.datasource.sga.SmartGatewaysAdapter;
import energy.eddie.aiida.adapters.datasource.shelly.ShellyAdapter;
import energy.eddie.aiida.adapters.datasource.simulation.SimulationAdapter;
import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.models.datasource.interval.modbus.ModbusDataSource;
import energy.eddie.aiida.models.datasource.interval.simulation.SimulationDataSource;
import energy.eddie.aiida.models.datasource.mqtt.at.OesterreichsEnergieDataSource;
import energy.eddie.aiida.models.datasource.mqtt.fr.MicroTeleinfoV3DataSource;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.datasource.mqtt.it.SinapsiAlfaDataSource;
import energy.eddie.aiida.models.datasource.mqtt.sga.SmartGatewaysDataSource;
import energy.eddie.aiida.models.datasource.mqtt.shelly.ShellyDataSource;
import energy.eddie.aiida.services.ModbusDeviceService;
import energy.eddie.aiida.services.secrets.SecretsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DataSourceAdapterTest {
    private ObjectMapper mapper;
    private MqttConfiguration mqttConfiguration;
    @Mock
    private SecretsService secretsService;

    @BeforeEach
    void setUp() {
        mapper = new AiidaConfiguration().customObjectMapper().build();
        mqttConfiguration = new MqttConfiguration(
                "tcp://localhost:1883",
                "tcp://localhost:1883",
                10,
                "password",
                ""
        );
    }

    @Test
    void givenOesterreichsEnergie_returnsAdapter() {
        // Given
        var dataSource = mock(OesterreichsEnergieDataSource.class);

        // When
        var adapter = DataSourceAdapter.create(dataSource, mapper, mqttConfiguration, secretsService);

        // Then
        assertInstanceOf(OesterreichsEnergieAdapter.class, adapter);
    }

    @Test
    void givenMicroTeleinfo_returnsAdapter() {
        // Given
        var dataSource = mock(MicroTeleinfoV3DataSource.class);
        when(dataSource.id()).thenReturn(UUID.randomUUID());
        when(dataSource.topic()).thenReturn("");

        // When
        var adapter = DataSourceAdapter.create(dataSource, mapper, mqttConfiguration, secretsService);

        // Then
        assertInstanceOf(MicroTeleinfoV3Adapter.class, adapter);
    }

    @Test
    void givenSinapsiAlfa_returnsAdapter() {
        // Given
        var dataSource = mock(SinapsiAlfaDataSource.class);

        // When
        var adapter = DataSourceAdapter.create(dataSource, mapper, mqttConfiguration, secretsService);

        // Then
        assertInstanceOf(SinapsiAlfaAdapter.class, adapter);
    }

    @Test
    void givenSmartGateways_returnsAdapter() {
        // Given
        var dataSource = mock(SmartGatewaysDataSource.class);
        when(dataSource.topic()).thenReturn("");

        // When
        var adapter = DataSourceAdapter.create(dataSource, mapper, mqttConfiguration, secretsService);

        // Then
        assertInstanceOf(SmartGatewaysAdapter.class, adapter);
    }

    @Test
    void givenShelly_returnsAdapter() {
        // Given
        var dataSource = mock(ShellyDataSource.class);

        // When
        var adapter = DataSourceAdapter.create(dataSource, mapper, mqttConfiguration, secretsService);

        // Then
        assertInstanceOf(ShellyAdapter.class, adapter);
    }

    @Test
    void givenInbound_returnsAdapter() {
        // Given
        var dataSource = mock(InboundDataSource.class);

        // When
        var adapter = DataSourceAdapter.create(dataSource, mapper, mqttConfiguration, secretsService);

        // Then
        assertInstanceOf(InboundAdapter.class, adapter);
    }

    @Test
    void givenSimulation_returnsAdapter() {
        // Given
        var dataSource = mock(SimulationDataSource.class);

        // When
        var adapter = DataSourceAdapter.create(dataSource, mapper, mqttConfiguration, secretsService);

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

            var dataSource = mock(ModbusDataSource.class);

            var adapter = DataSourceAdapter.create(dataSource, mapper, mqttConfiguration, secretsService);

            assertInstanceOf(ModbusTcpDataSourceAdapter.class, adapter);
        }
    }
}
