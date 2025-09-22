package energy.eddie.regionconnector.at.eda.handlers.integration.inbound;

import energy.eddie.regionconnector.at.api.AtPermissionRequestProjection;

import java.time.Instant;
import java.time.LocalDate;

public record AtPermissionRequestProjectionTest(
        String permissionId,
        String connectionId,
        String cmRequestId,
        String conversationId,
        LocalDate permissionStart,
        LocalDate permissionEnd,
        String dataNeedId,
        String dsoId,
        String meteringPointId,
        String consentId,
        String message,
        String granularity,
        String status,
        Instant created
) implements AtPermissionRequestProjection {
    @Override public String getPermissionId() { return permissionId; }
    @Override public String getConnectionId() { return connectionId; }
    @Override public String getCmRequestId() { return cmRequestId; }
    @Override public String getConversationId() { return conversationId; }
    @Override public LocalDate getPermissionStart() { return permissionStart; }
    @Override public LocalDate getPermissionEnd() { return permissionEnd; }
    @Override public String getDataNeedId() { return dataNeedId; }
    @Override public String getDsoId() { return dsoId; }
    @Override public String getMeteringPointId() { return meteringPointId; }
    @Override public String getConsentId() { return consentId; }
    @Override public String getMessage() { return message; }
    @Override public String getGranularity() { return granularity; }
    @Override public String getStatus() { return status; }
    @Override public Instant getCreated() { return created; }
}