package energy.eddie.regionconnector.at.eda;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.EdaDataSourceInformation;

import java.time.ZonedDateTime;
import java.util.Optional;

public record SimplePermissionRequest(String permissionId,
                                      String connectionId,
                                      String dataNeedId,
                                      String cmRequestId,
                                      String conversationId,
                                      String dsoId,
                                      Optional<String> meteringPointId,
                                      ZonedDateTime start,
                                      ZonedDateTime end,
                                      PermissionProcessStatus status
) implements AtPermissionRequest {

    public SimplePermissionRequest(String permissionId, String connectionId) {
        this(permissionId, connectionId, null, null, null, null, Optional.empty(), null, null, null);
    }

    public SimplePermissionRequest(String permissionId, String connectionId, String dataNeedId) {
        this(permissionId, connectionId, dataNeedId, null, null, null, Optional.empty(), null, null, null);
    }

    public SimplePermissionRequest(String permissionId, String connectionId, String dataNeedId, String cmRequestId,
                                   String conversationId, PermissionProcessStatus status) {
        this(permissionId, connectionId, dataNeedId, cmRequestId, conversationId, null, Optional.empty(), null, null,
             status);
    }

    @Override
    public Optional<String> consentId() {
        return Optional.empty();
    }

    @Override
    public PermissionRequestState state() {
        return null;
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
    public void changeState(PermissionRequestState state) {

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
    public String message() {
        return null;
    }

    @Override
    public Granularity granularity() {
        return Granularity.PT15M;
    }
}