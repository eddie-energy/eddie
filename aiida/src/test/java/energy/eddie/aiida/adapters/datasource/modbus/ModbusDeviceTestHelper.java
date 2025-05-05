package energy.eddie.aiida.adapters.datasource.modbus;

import energy.eddie.aiida.models.modbus.*;

import java.util.List;
import java.util.Map;

public class ModbusDeviceTestHelper {
    public static ModbusDevice setupModbusDevice() {
        return new ModbusDevice(
                "inverter-1",
                "Inverter ABC",
                1502,
                1,
                new Intervals(new IntervalConfig(5000, 1000)),
                List.of(
                        new ModbusSource(SourceCategory.INVERTER, "inverter-2", List.of(
                                new ModbusDataPoint("status", 10, RegisterType.HOLDING, 1, "uint16", Endian.BIG, false, null, null,
                                        Map.of("1", "ON", "2", "STANDBY", "3", "FAULT", "default", "UNKNOWN"), Access.READ)
                        )),
                        new ModbusSource(SourceCategory.INVERTER, "inverter-1", List.of(
                                new ModbusDataPoint("status", 10, RegisterType.HOLDING, 1, "uint16", Endian.BIG, false, null, null,
                                        Map.of("1", "ON", "2", "STANDBY", "3", "FAULT", "default", "UNKNOWN"), Access.READ),
                                new ModbusDataPoint("firmware_version", 11, RegisterType.HOLDING, 5, "string", Endian.BIG, false, null, null, null, Access.READ),
                                new ModbusDataPoint("start_command", 0, RegisterType.COIL, 1, "boolean", Endian.BIG, false, null, null, null, Access.READWRITE),
                                new ModbusDataPoint("stop_command", 1, RegisterType.COIL, 1, "boolean", Endian.BIG, false, null, null, null, Access.WRITE)
                        )),
                        new ModbusSource(SourceCategory.BATTERY, "battery-1", List.of(
                                new ModbusDataPoint("state_of_charge_lit", 20, RegisterType.HOLDING, 2, "float32", Endian.LITTLE, false, null, null, null, Access.READ),
                                new ModbusDataPoint("state_of_charge_big", 22, RegisterType.HOLDING, 2, "float32", Endian.BIG, false, null, "(@self / 100)", null, Access.READ),
                                new ModbusDataPoint("charging_power", 24, RegisterType.HOLDING, 2, "int32", Endian.BIG, false, null, null, null, Access.READ),
                                new ModbusDataPoint("force_charge", 5, RegisterType.COIL, 1, "boolean", Endian.BIG, false, null, null, null, Access.READWRITE),
                                new ModbusDataPoint("discharge_enable", 6, RegisterType.COIL, 1, "boolean", Endian.BIG, false, null, null, null, Access.READWRITE)
                        )),
                        new ModbusSource(SourceCategory.ELECTRICITY_METER_AC, "electricity_meter-1", List.of(
                                new ModbusDataPoint("voltage_l1", 20, RegisterType.INPUT, 2, "float32", Endian.LITTLE, false, null, null, null, Access.READWRITE),
                                new ModbusDataPoint("voltage_l2", 22, RegisterType.INPUT, 2, "float32", Endian.BIG, false, null, null, null, Access.READWRITE),
                                new ModbusDataPoint("voltage_l3", 24, RegisterType.INPUT, 2, "float32", Endian.BIG, false, null, null, null, Access.READ),
                                new ModbusDataPoint("current_l1", 26, RegisterType.INPUT, 2, "float32", Endian.LITTLE, false, null, null, null, Access.READ),
                                new ModbusDataPoint("current_l2", 28, RegisterType.INPUT, 2, "float32", Endian.BIG, false, null, null, null, Access.READ),
                                new ModbusDataPoint("current_l3", 30, RegisterType.INPUT, 2, "float32", Endian.BIG, false, null, null, null, Access.READ),
                                new ModbusDataPoint("breaker_closed", 10, RegisterType.DISCRETE, 1, "boolean", Endian.BIG, false, null, null, null, Access.READ),
                                new ModbusDataPoint("grid_sync", 11, RegisterType.DISCRETE, 1, "boolean", Endian.BIG, false, null, null, null, Access.READ),
                                new ModbusDataPoint("power_total", 0, RegisterType.UNKNOWN, 0, null, Endian.UNKNOWN, true,
                                        List.of("voltage_l1", "current_l1", "voltage_l2", "current_l2", "voltage_l3", "current_l3"),
                                        "(@voltage_l1 * @current_l1) + (@voltage_l2 * @current_l2) + (@voltage_l3 * @current_l3)", null, Access.READ),
                                new ModbusDataPoint("power_total_soc", 0, RegisterType.UNKNOWN, 0, null, Endian.UNKNOWN, true,
                                        List.of("power_total", "battery-1::state_of_charge_lit"),
                                        "@power_total + @battery-1::state_of_charge_lit", null, Access.READ),
                                new ModbusDataPoint("is_error", 0, RegisterType.UNKNOWN, 0, null, Endian.UNKNOWN, true,
                                        List.of("power_total", "battery-1::state_of_charge_lit"),
                                        "(@power_total + @battery-1::state_of_charge_lit) > 3000", null, Access.READ),
                                new ModbusDataPoint("error_code", 0, RegisterType.UNKNOWN, 0, null, Endian.UNKNOWN, true,
                                        List.of("power_total", "battery-1::state_of_charge_lit"),
                                        "(@power_total + @battery-1::state_of_charge_lit) < 8000 ? 'low_error' : (@power_total + @battery-1::state_of_charge_lit) < 8750 ? 'medium_error' : 'high_error'", null, Access.READ)
                        ))
                )
        );
    }
}
