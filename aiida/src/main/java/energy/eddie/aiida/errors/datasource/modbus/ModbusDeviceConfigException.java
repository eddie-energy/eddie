package energy.eddie.aiida.errors.datasource.modbus;

public class ModbusDeviceConfigException extends RuntimeException {
    public ModbusDeviceConfigException(String message) {
        super(message);
    }

    public ModbusDeviceConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
