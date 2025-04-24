package energy.eddie.regionconnector.at.api;

import java.time.LocalDate;
import java.time.ZonedDateTime;

public interface AtPermissionRequestProjection {
    String getPermissionId();
    String getConnectionId();
    String getCmRequestId();
    String getConversationId();
    LocalDate getStart();
    LocalDate getEnd();
    String getDataNeedId();
    String getDsoId();
    String getMeteringPointId();
    String getConsentId();
    String getMessage();
    String getGranularity();
    String getStatus();
    ZonedDateTime getCreated();
}
