package energy.eddie.regionconnector.at.eda.models;

/**
 * This record represents a response code from EDA in the context of "Consent Management". The documentation for the
 * response codes can be found <a href="https://www.ebutilities.at/responsecodes">here</a>
 *
 * @param responseCode The response code
 * @param message      The message associated with the response code
 */
public record ResponseCode(int responseCode, String message) {

    public ResponseCode(int responseCode) {
        this(responseCode, getMessage(responseCode));
    }

    private static String getMessage(int responseCode) {
        return switch (responseCode) {
            case 56 -> "Metering point not found";
            case 57 -> "Metering point not supplied";
            case 76 -> "Invalid request data";
            case 82 -> "Invalid dates";
            case 99 -> "Message received";
            case 172 -> "Customer rejected the request";
            case 173 -> "Time-out";
            case 174 -> "Requested data not deliverable";
            case 175 -> "Customer accepted the request";
            case 176 -> "Consent successfully withdrawn";
            case 177 -> "No data sharing available";
            case 178 -> "Consent already exists";
            case 179 -> "ConsentId already exists";
            case 180 -> "ConsentId expired";
            case 187 -> "ConsentId and MeteringPointId are not associated";
            default -> "Unknown response code";
        };
    }

    @Override
    public String toString() {
        return message + " (response code " + responseCode + ")";
    }
}
