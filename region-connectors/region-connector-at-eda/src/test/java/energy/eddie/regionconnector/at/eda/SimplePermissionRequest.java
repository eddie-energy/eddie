package energy.eddie.regionconnector.at.eda;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestProjection;
import energy.eddie.regionconnector.at.eda.permission.request.EdaDataSourceInformation;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record SimplePermissionRequest(String permissionId,
                                      String connectionId,
                                      String dataNeedId,
                                      String cmRequestId,
                                      String conversationId,
                                      String dsoId,
                                      Optional<String> meteringPointId,
                                      LocalDate start,
                                      LocalDate end,
                                      PermissionProcessStatus status,
                                      Optional<String> consentId,
                                      @Nullable AllowedGranularity granularity
) implements AtPermissionRequest, AtPermissionRequestProjection {

    public SimplePermissionRequest(String permissionId, String connectionId, String dataNeedId) {
        this(permissionId,
             connectionId,
             dataNeedId,
             null,
             null,
             null,
             Optional.empty(),
             null,
             null,
             null,
             Optional.empty());
    }

    public SimplePermissionRequest(
            String permissionId, String connectionId, String dataNeedId, String cmRequestId,
            String conversationId, PermissionProcessStatus status
    ) {
        this(permissionId, connectionId, dataNeedId, cmRequestId, conversationId, null, Optional.empty(), null, null,
             status, Optional.empty());
    }

    public SimplePermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            String cmRequestId,
            String conversationId,
            String dsoId,
            Optional<String> meteringPointId,
            LocalDate start,
            LocalDate end,
            PermissionProcessStatus status,
            Optional<String> consentId
    ) {
        this(permissionId, connectionId, dataNeedId, cmRequestId, conversationId, dsoId, meteringPointId, start, end,
             status, consentId, AllowedGranularity.PT15M);
    }

    @Override
    public Optional<String> consentId() {
        return consentId;
    }

    @Override
    public String message() {
        return null;
    }

    @Override
    public AllowedGranularity granularity() {
        return granularity;
    }

    @Override
    public DataSourceInformation dataSourceInformation() {
        return new EdaDataSourceInformation(dsoId);
    }

    @Override
    public ZonedDateTime created() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AtPermissionRequest that)) return false;
        return permissionId.equals(that.permissionId()) && connectionId.equals(
                that.connectionId()) && cmRequestId.equals(that.cmRequestId()) && conversationId.equals(
                that.conversationId()) && status == that.status();
    }

    @Override
    public String getPermissionId() {
        return permissionId;
    }

    @Override
    public String getConnectionId() {
        return connectionId;
    }

    @Override
    public String getCmRequestId() {
        return cmRequestId;
    }

    @Override
    public String getConversationId() {
        return conversationId;
    }

    @Override
    public LocalDate getStart() {
        return start;
    }

    @Override
    public LocalDate getEnd() {
        return end;
    }

    @Override
    public String getDataNeedId() {
        return dataNeedId;
    }

    @Override
    public String getDsoId() {
        return dsoId;
    }

    @Override
    public String getMeteringPointId() {
        return String.valueOf(meteringPointId);
    }

    @Override
    public String getConsentId() {
        return String.valueOf(consentId);
    }

    @Override
    public String getMessage() {
        return null;
    }

    @Override
    public String getGranularity() {
        return String.valueOf(granularity());
    }

    @Override
    public String getStatus() {
        return String.valueOf(status);
    }

    @Override
    public ZonedDateTime getCreated() {
        return created();
    }
}
