package energy.eddie.aiida.errors.datasource;

public class InvalidDataSourceTypeException extends Exception {
    public InvalidDataSourceTypeException() {
        super("Invalid data source type.");
    }
}
