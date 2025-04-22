package energy.eddie.aiida.errors;

public class ModbusConnectionException extends RuntimeException {
    public ModbusConnectionException(String message, Exception cause) {
        super(message, cause);
    }
}
