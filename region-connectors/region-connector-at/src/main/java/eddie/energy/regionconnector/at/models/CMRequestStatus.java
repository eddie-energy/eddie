package eddie.energy.regionconnector.at.models;

public class CMRequestStatus {

    private final Status status;
    private final String message;


    private String requestId;

    public CMRequestStatus(Status status, String message, String requestId) {
        this.status = status;
        this.message = message;
        this.requestId = requestId;
    }

    public CMRequestStatus(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    @Override
    public String toString() {
        return "{ status=" + status + ", message='" + message + '}';
    }


    public enum Status {
        SENT,
        RECEIVED,
        DELIVERED,
        ACCEPTED,
        REJECTED,
        ERROR
    }
}
