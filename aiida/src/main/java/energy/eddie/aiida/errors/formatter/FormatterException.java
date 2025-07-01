package energy.eddie.aiida.errors.formatter;

public abstract class FormatterException extends RuntimeException {
    protected FormatterException(Exception exception) {
        super(exception);
    }
}
