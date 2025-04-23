package energy.eddie.aiida.adapters.datasource.modbus;

import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceModbusDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.modbus.ModbusDataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.modbus.ModbusDevice;
import energy.eddie.aiida.models.modbus.ModbusDataPoint;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.services.ModbusDeviceService;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModbusTcpDataSourceAdapterTest {

    private ModbusTcpDataSourceAdapter modbusTcpDataSourceAdapter;

    @Mock
    private ModbusTcpClient mockModbusTcpClient;

    @BeforeEach
    void setUp() {
        // Prepare your ModbusDevice using your helper
        ModbusDevice modbusDevice = ModbusDeviceTestHelper.setupModbusDevice();

        // Setup DataSource (customize fields if needed)
        DataSourceDto dto = new DataSourceDto(
                UUID.randomUUID(),                          // id
                DataSourceType.MODBUS,                      // data source type
                AiidaAsset.DEDICATED_MEASUREMENT_DEVICE,    // asset
                "test-datasource",                          // name
                true,                                       // enabled
                1,                                          // simulationPeriod (pollingInterval in seconds)
                null,                                       // mqttSettings
                new DataSourceModbusDto(                    // modbusSettings
                        "127.0.0.1",
                        null,
                        null,
                        UUID.randomUUID()  // modbusDevice UUID
                )
        );

        UUID userId = UUID.randomUUID(); // Replace as needed for tests

        if (dto.modbusSettings() == null) return;

        ModbusDataSource dataSource = new ModbusDataSource(dto, userId, dto.modbusSettings());

        // Mock the static call
        try (MockedStatic<ModbusDeviceService> mockedDeviceService = Mockito.mockStatic(ModbusDeviceService.class)) {
            mockedDeviceService.when(() -> ModbusDeviceService.loadConfig(any(UUID.class)))
                    .thenReturn(modbusDevice);

            // instantiate the adapter within this block
            modbusTcpDataSourceAdapter = new ModbusTcpDataSourceAdapter(dataSource);
        }

        modbusTcpDataSourceAdapter.setModbusClientHelper(mockModbusTcpClient);
    }

    @Test
    void testModbusAdapterEmitsValuesOnStart() {
        // Mock the behavior of ModbusClientHelper to return dummy values
        when(mockModbusTcpClient.readHoldingRegister(any(ModbusDataPoint.class)))
                .thenReturn(Optional.of(123));

        when(mockModbusTcpClient.readInputRegister(any(ModbusDataPoint.class)))
                .thenReturn(Optional.of(456));

        when(mockModbusTcpClient.readCoil(any(ModbusDataPoint.class)))
                .thenReturn(Optional.of(true));

        when(mockModbusTcpClient.readDiscreteInput(any(ModbusDataPoint.class)))
                .thenReturn(Optional.of(false));

        // Start the Flux stream from the adapter
        Flux<AiidaRecord> recordFlux = modbusTcpDataSourceAdapter.start();

        // Verify using StepVerifier
        StepVerifier.create(recordFlux)
                .thenAwait(java.time.Duration.ofSeconds(2))
                .assertNext(recordValues -> {
                    List<AiidaRecordValue> values = recordValues.aiidaRecordValues();

                    assertThat(values).hasSize(21);

                    assertThat(values).extracting(AiidaRecordValue::dataPointKey, AiidaRecordValue::value).containsExactlyInAnyOrder(
                            tuple("inverter-2::status", "UNKNOWN"),
                            tuple("inverter-1::status", "UNKNOWN"),
                            tuple("inverter-1::firmware_version", "123"),
                            tuple("inverter-1::start_command", "true"),
                            tuple("battery-1::state_of_charge_lit", "123"),
                            tuple("battery-1::state_of_charge_big", "1.23"),
                            tuple("battery-1::charging_power", "123"),
                            tuple("battery-1::force_charge", "true"),
                            tuple("battery-1::discharge_enable", "true"),
                            tuple("electricity_meter-1::voltage_l1", "456"),
                            tuple("electricity_meter-1::voltage_l2", "456"),
                            tuple("electricity_meter-1::voltage_l3", "456"),
                            tuple("electricity_meter-1::current_l1", "456"),
                            tuple("electricity_meter-1::current_l2", "456"),
                            tuple("electricity_meter-1::current_l3", "456"),
                            tuple("electricity_meter-1::breaker_closed", "false"),
                            tuple("electricity_meter-1::grid_sync", "false"),
                            tuple("electricity_meter-1::power_total", "623808"),
                            tuple("electricity_meter-1::power_total_soc", "623931"),
                            tuple("electricity_meter-1::is_error", "true"),
                            tuple("electricity_meter-1::error_code", "high_error")
                    );

                })
                .thenCancel()
                .verify();
    }

    @Test
    void testCloseDisposesResources() {
        // Spy on an instance, bypassing real TCP logic
        ModbusTcpClient spyClient = Mockito.mock(ModbusTcpClient.class);

        modbusTcpDataSourceAdapter.setModbusClientHelper(spyClient);
        modbusTcpDataSourceAdapter.start();
        modbusTcpDataSourceAdapter.close();

        Mockito.verify(spyClient).close(); // ✅ verify cleanup
    }



}


