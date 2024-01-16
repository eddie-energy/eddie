package energy.eddie.core.dataneeds.exceptions;

public class DataNeedIdsNotMatchingException extends Exception {
    public DataNeedIdsNotMatchingException() {
        super("Data need ID in URL does not match data need ID in request body.");
    }
}
