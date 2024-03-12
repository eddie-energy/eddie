package energy.eddie.api.agnostic.exceptions;

public class DataNeedNotFoundException extends Exception {
    private final boolean isBadRequest;

    /**
     * Creates a new {@link DataNeedNotFoundException} that results in a HTTP status code 400.
     *
     * @param dataNeedId ID of the dataNeed that couldn't be found.
     */
    public DataNeedNotFoundException(String dataNeedId) {
        this(dataNeedId, true);
    }

    /**
     * {@code isBadRequest} should be used to indicated that the Exception occurred because e.g. the permission request
     * didn't contain a valid dataNeedId. If it's false, 404 is used (e.g. when dataNeedId is a path param).
     *
     * @param dataNeedId   ID of the dataNeed that couldn't be found.
     * @param isBadRequest If true, HTTP status code 400 is returned, otherwise 404.
     */
    public DataNeedNotFoundException(String dataNeedId, boolean isBadRequest) {
        super("No data need with ID '%s' found.".formatted(dataNeedId));
        this.isBadRequest = isBadRequest;
    }

    public boolean isBadRequest() {
        return isBadRequest;
    }
}