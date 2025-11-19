package energy.eddie.aiida.adapters.datasource.modbus;

import com.ghgande.j2mod.modbus.msg.ModbusRequest;

@FunctionalInterface
public interface ModbusRequestSupplier {
    ModbusRequest get(int register, int length);
}
