package energy.eddie.regionconnector.at.eda.models;

import jakarta.annotation.Nullable;

import java.util.Objects;
import java.util.Optional;

public final class CMRequestStatus {
    private final Status status;
    private final String message;
    private final String conversationId;

    @Nullable
    private String cmRequestId;

    @Nullable
    private String cmConsentId;

    @Nullable
    private String meteringPoint;

    public CMRequestStatus(Status status, String message, String conversationId) {
        this.status = status;
        this.message = message;
        this.conversationId = conversationId;
    }

    @Override
    public String toString() {
        return "{ status=\"" + status + "\", message=\"" + message + "\", conversationId=\"" +
                conversationId + "\"" + ", cmRequestId=\"" + cmRequestId + "\"" + ", cmConsentId=\"" + cmConsentId + "\"" +
                ", meteringPoint=\"" + meteringPoint + "\""
                + " }";
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getConversationId() {
        return conversationId;
    }

    public Optional<String> getCMRequestId() {
        return Optional.ofNullable(cmRequestId);
    }

    public Optional<String> getCMConsentId() {
        return Optional.ofNullable(cmConsentId);
    }

    public Optional<String> getMeteringPoint() {
        return Optional.ofNullable(meteringPoint);
    }

    public void setMeteringPoint(@Nullable String meteringPoint) {
        this.meteringPoint = meteringPoint;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CMRequestStatus that = (CMRequestStatus) o;
        return status == that.status && Objects.equals(conversationId, that.conversationId) && Objects.equals(cmRequestId, that.cmRequestId) && Objects.equals(cmConsentId, that.cmConsentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, conversationId, cmRequestId, cmConsentId);
    }

    public void setCmRequestId(@Nullable String cmRequestId) {
        this.cmRequestId = cmRequestId;
    }

    public void setCmConsentId(@Nullable String cmConsentId) {
        this.cmConsentId = cmConsentId;
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
