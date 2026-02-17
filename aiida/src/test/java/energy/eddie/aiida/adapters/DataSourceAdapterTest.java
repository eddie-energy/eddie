// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters;

import energy.eddie.aiida.ObjectMapperCreatorUtil;
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
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.interval.modbus.ModbusDataSource;
import energy.eddie.aiida.models.datasource.interval.simulation.SimulationDataSource;
import energy.eddie.aiida.models.datasource.mqtt.at.OesterreichsEnergieDataSource;
import energy.eddie.aiida.models.datasource.mqtt.fr.MicroTeleinfoV3DataSource;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.datasource.mqtt.it.SinapsiAlfaDataSource;
import energy.eddie.aiida.models.datasource.mqtt.sga.SmartGatewaysDataSource;
import energy.eddie.aiida.models.datasource.mqtt.shelly.ShellyDataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.services.ModbusDeviceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DataSourceAdapterTest {
    private ObjectMapper mapper;
    private MqttConfiguration mqttConfiguration;

    @BeforeEach
    void setUp() {
        mapper = ObjectMapperCreatorUtil.mapper();
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
        var adapter = DataSourceAdapter.create(dataSource, mapper, mqttConfiguration);

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
        var adapter = DataSourceAdapter.create(dataSource, mapper, mqttConfiguration);

        // Then
        assertInstanceOf(MicroTeleinfoV3Adapter.class, adapter);
    }

    @Test
    void givenSinapsiAlfa_returnsAdapter() {
        // Given
        var dataSource = mock(SinapsiAlfaDataSource.class);

        // When
        var adapter = DataSourceAdapter.create(dataSource, mapper, mqttConfiguration);

        // Then
        assertInstanceOf(SinapsiAlfaAdapter.class, adapter);
    }

    @Test
    void givenSmartGateways_returnsAdapter() {
        // Given
        var dataSource = mock(SmartGatewaysDataSource.class);
        when(dataSource.topic()).thenReturn("");

        // When
        var adapter = DataSourceAdapter.create(dataSource, mapper, mqttConfiguration);

        // Then
        assertInstanceOf(SmartGatewaysAdapter.class, adapter);
    }

    @Test
    void givenShelly_returnsAdapter() {
        // Given
        var dataSource = mock(ShellyDataSource.class);

        // When
        var adapter = DataSourceAdapter.create(dataSource, mapper, mqttConfiguration);

        // Then
        assertInstanceOf(ShellyAdapter.class, adapter);
    }

    @Test
    void givenInbound_returnsAdapter() {
        // Given
        var dataSource = mock(InboundDataSource.class);

        // When
        var adapter = DataSourceAdapter.create(dataSource, mapper, mqttConfiguration);

        // Then
        assertInstanceOf(InboundAdapter.class, adapter);
    }

    @Test
    void givenSimulation_returnsAdapter() {
        // Given
        var dataSource = mock(SimulationDataSource.class);

        // When
        var adapter = DataSourceAdapter.create(dataSource, mapper, mqttConfiguration);

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

            var adapter = DataSourceAdapter.create(dataSource, mapper, mqttConfiguration);

            assertInstanceOf(ModbusTcpDataSourceAdapter.class, adapter);
        }
    }

    @Test
    void testHealth() {
        //noinspection resource
        var adapter = new HealthTestDataSourceAdapter(mock(DataSource.class));

        assertEquals(Status.UNKNOWN, Objects.requireNonNull(adapter.health()).getStatus());

        var record1 = mock(AiidaRecord.class);
        var record2 = mock(AiidaRecord.class);
        var record3 = mock(AiidaRecord.class);
        var now = Instant.now();

        when(record1.timestamp()).thenReturn(now.minusSeconds(181));
        when(record2.timestamp()).thenReturn(now.minusSeconds(120));
        when(record3.timestamp()).thenReturn(now.minusSeconds(10));

        adapter.addTestHealthValidationMessage(record1);
        assertEquals(Status.DOWN, Objects.requireNonNull(adapter.health()).getStatus());

        adapter.addTestHealthValidationMessage(record2);
        assertEquals(Health.status("WARNING").build().getStatus(), Objects.requireNonNull(adapter.health()).getStatus());

        adapter.addTestHealthValidationMessage(record3);
        assertEquals(Status.UP, Objects.requireNonNull(adapter.health()).getStatus());
    }

    private static class HealthTestDataSourceAdapter extends DataSourceAdapter<DataSource> {

        protected HealthTestDataSourceAdapter(DataSource dataSource) {
            super(dataSource);
        }

        @Override
        public Flux<AiidaRecord> start() {
            return Flux.empty();
        }

        @Override
        public void close() {
            // Not needed for test setup
        }

        public void addTestHealthValidationMessage(AiidaRecord r) {
            healthValidationMessages.addFirst(r);
        }
    }
}
