package energy.eddie.aiida.adapters.datasource.modbus;

import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceModbusDto;
import energy.eddie.aiida.models.datasource.DataSourceIcon;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.modbus.ModbusDataSource;
import energy.eddie.aiida.models.modbus.ModbusDevice;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.services.ModbusDeviceService;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
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
    private MockedStatic<ModbusDeviceService> mockedDeviceService;
    private ModbusDevice modbusDevice;
    private ModbusTcpDataSourceAdapter adapter;
    private MockedConstruction<ModbusTcpClient> mockedClientConstruction;
    private ModbusTcpClient mockClient;

    @BeforeEach
    void setUp() throws Exception {
        modbusDevice = ModbusDeviceTestHelper.setupModbusDevice();

        mockedDeviceService = Mockito.mockStatic(ModbusDeviceService.class);
        mockedDeviceService.when(() -> ModbusDeviceService.loadConfig(any(UUID.class)))
                .thenReturn(modbusDevice);

        // Construct mock client before adapter instantiation (intercepts new ModbusTcpClient(...))
        mockedClientConstruction = Mockito.mockConstruction(ModbusTcpClient.class,
                (mock, context) -> {
                    when(mock.readHoldingRegister(any())).thenReturn(Optional.of(123));
                    when(mock.readInputRegister(any())).thenReturn(Optional.of(456));
                    when(mock.readCoil(any())).thenReturn(Optional.of(true));
                    when(mock.readDiscreteInput(any())).thenReturn(Optional.of(false));
                }
        );

        DataSourceDto dto = new DataSourceDto(
                UUID.randomUUID(),
                DataSourceType.MODBUS,
                AiidaAsset.DEDICATED_MEASUREMENT_DEVICE,
                "test-datasource",
                "AT",
                true,
                DataSourceIcon.METER,
                1,
                null,
                new DataSourceModbusDto("127.0.0.1", null, null, UUID.randomUUID())
        );

        ModbusDataSource dataSource = new ModbusDataSource(dto, UUID.randomUUID(), dto.modbusSettings());
        adapter = new ModbusTcpDataSourceAdapter(dataSource);

        // Grab the actual mock instance created by the constructor
        mockClient = mockedClientConstruction.constructed().get(0);
    }

    @AfterEach
    void tearDown() {
        if (mockedDeviceService != null) mockedDeviceService.close();
        if (mockedClientConstruction != null) mockedClientConstruction.close();
    }

    @Test
    void testModbusAdapterEmitsValuesOnStart() {
        Flux<AiidaRecord> flux = adapter.start();

        StepVerifier.create(flux)
                .thenAwait(java.time.Duration.ofSeconds(2))
                .assertNext(aiidaRecord -> {
                    List<AiidaRecordValue> values = aiidaRecord.aiidaRecordValues();
                    assertThat(values).hasSize(21);
                    assertThat(values)
                            .extracting(AiidaRecordValue::dataPointKey, AiidaRecordValue::value)
                            .containsExactlyInAnyOrderElementsOf(expectedModbusRecordTuples());
                })
                .thenCancel()
                .verify();

    }

    @Test
    void testCloseDisposesResources() {
        adapter.start();
        adapter.close();

        Mockito.verify(mockClient).close();
    }

    private List<Tuple> expectedModbusRecordTuples() {
        return List.of(
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
    }
}