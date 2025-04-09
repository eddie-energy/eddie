package energy.eddie.aiida.models.modbus;

import javax.annotation.Nullable;
import java.util.List;

public class DeviceConfigWrapper {
    @Nullable
    private ModbusWrapper modbus;

    @Nullable
    public ModbusWrapper getModbus() {
        return modbus;
    }

    public void setModbus(@Nullable ModbusWrapper modbus) {
        this.modbus = modbus;
    }

    public List<ModbusDevice> getDevices() {
        return modbus != null ? modbus.getDevices() : List.of();
    }

    public static class ModbusWrapper {
        @Nullable
        private List<ModbusDevice> devices;

        public List<ModbusDevice> getDevices() {
            return devices != null ? devices : List.of();
        }
    }
}
