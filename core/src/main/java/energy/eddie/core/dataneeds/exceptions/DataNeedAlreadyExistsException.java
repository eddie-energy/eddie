package energy.eddie.core.dataneeds.exceptions;

public class DataNeedAlreadyExistsException extends Exception {
    public DataNeedAlreadyExistsException(String id) {
        super("Data need with ID '%s' already exists.".formatted(id));
    }
}
