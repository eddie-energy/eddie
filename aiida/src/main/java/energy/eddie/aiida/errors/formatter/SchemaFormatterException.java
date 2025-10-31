package energy.eddie.aiida.errors.formatter;

public abstract class SchemaFormatterException extends Exception {
    protected SchemaFormatterException(Exception exception) {
        super(exception);
    }
}
