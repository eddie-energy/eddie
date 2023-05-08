package eddie.energy.regionconnector.at.models;

public record CMRequestStatus(Status status, String message, String conversationId) {

    @Override
    public String toString() {
        return "{ status=" + status + ", message='" + message + ", conversationId='" + conversationId + " }";
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
