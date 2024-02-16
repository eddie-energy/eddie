package energy.eddie.regionconnector.dk.energinet.exceptions;

public class ApiResponseException extends RuntimeException {
    public ApiResponseException(Integer errorCode, String errorText) {
        super("Error code: " + errorCode + ", error text: " + errorText);
    }
}