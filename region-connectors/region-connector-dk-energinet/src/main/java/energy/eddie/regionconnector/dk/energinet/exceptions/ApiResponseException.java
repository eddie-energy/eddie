package energy.eddie.regionconnector.dk.energinet.exceptions;

public class ApiResponseException extends RuntimeException {
    private final String errorText;
    private final int errorCode;

    public ApiResponseException(Integer errorCode, String errorText) {
        super("Error code: " + errorCode + ", error text: " + errorText);
        this.errorCode = errorCode;
        this.errorText = errorText;
    }

    public int errorCode() {
        return errorCode;
    }

    public String errorText() {
        return errorText;
    }
}
