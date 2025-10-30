package energy.eddie.aiida.errors.datasource.modbus;

public class ModbusConnectionException extends RuntimeException {
    public ModbusConnectionException(String message, Exception cause) {
        super(message, cause);
    }
}
