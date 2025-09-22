package energy.eddie.regionconnector.at.api;

import java.time.Instant;
import java.time.LocalDate;

public interface AtPermissionRequestProjection {
    String getPermissionId();
    String getConnectionId();
    String getCmRequestId();
    String getConversationId();
    LocalDate getPermissionStart();
    LocalDate getPermissionEnd();
    String getDataNeedId();
    String getDsoId();
    String getMeteringPointId();
    String getConsentId();
    String getMessage();
    String getGranularity();
    String getStatus();
    Instant getCreated();
}
