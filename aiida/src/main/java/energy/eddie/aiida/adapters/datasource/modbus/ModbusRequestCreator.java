package energy.eddie.aiida.adapters.datasource.modbus;

import com.ghgande.j2mod.modbus.msg.ModbusRequest;

@FunctionalInterface
public interface ModbusRequestCreator {
    ModbusRequest create(int register, int length);
}
