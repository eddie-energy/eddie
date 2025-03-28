package energy.eddie.aiida.exceptions;

public class ModbusDeviceConfigException extends RuntimeException {
    public ModbusDeviceConfigException(String message) {
        super(message);
    }

    public ModbusDeviceConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
