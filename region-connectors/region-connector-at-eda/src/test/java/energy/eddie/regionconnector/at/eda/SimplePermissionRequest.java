package energy.eddie.regionconnector.at.eda;

import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.DataSourceInformation;
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
                                      PermissionRequestState state
) implements AtPermissionRequest {

    public SimplePermissionRequest(String permissionId, String connectionId) {
        this(permissionId, connectionId, null, null, null, null, Optional.empty(), null, null, null);
    }

    public SimplePermissionRequest(String permissionId, String connectionId, String dataNeedId, String cmRequestId, String conversationId, PermissionRequestState state) {
        this(permissionId, connectionId, dataNeedId, cmRequestId, conversationId, null, Optional.empty(), null, null, state);
    }


    @Override
    public Optional<String> consentId() {
        return Optional.empty();
    }

    @Override
    public DataSourceInformation dataSourceInformation() {
        return new EdaDataSourceInformation(dsoId);
    }

    @Override
    public void changeState(PermissionRequestState state) {

    }

    @Override
    public void validate() {

    }

    @Override
    public void sendToPermissionAdministrator() {

    }

    @Override
    public void receivedPermissionAdministratorResponse() {

    }

    @Override
    public void terminate() {

    }

    @Override
    public void accept() {

    }

    @Override
    public void invalid() {

    }

    @Override
    public void reject() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AtPermissionRequest that)) return false;
        return permissionId.equals(that.permissionId()) && connectionId.equals(that.connectionId()) && cmRequestId.equals(that.cmRequestId()) && conversationId.equals(that.conversationId()) && state == that.state();
    }

    @Override
    public String stateTransitionMessage() {
        return null;
    }

    @Override
    public void setStateTransitionMessage(String message) {

    }

    @Override
    public void setMeteringPointId(String meteringPointId) {

    }

    @Override
    public void setConsentId(String consentId) {
        // No-Op
    }
}