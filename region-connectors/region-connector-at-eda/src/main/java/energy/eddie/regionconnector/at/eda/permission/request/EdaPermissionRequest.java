package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.permission.request.states.AtCreatedPermissionRequestState;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class EdaPermissionRequest implements AtPermissionRequest {
    private final String connectionId;
    private final String permissionId;
    private final String cmRequestId;
    private final String conversationId;
    private final LocalDate dataFrom;
    @Nullable
    private final LocalDate dataTo;
    private final String dataNeedId;
    private final EdaDataSourceInformation dataSourceInformation;
    @Nullable
    private String meteringPointId;
    private PermissionRequestState state;
    private String statusTransitionMessage = "";

    public EdaPermissionRequest(String connectionId, String dataNeedId, CCMORequest ccmoRequest, EdaAdapter edaAdapter) {
        this(connectionId, UUID.randomUUID().toString(), dataNeedId, ccmoRequest, edaAdapter);
    }

    public EdaPermissionRequest(String connectionId, String permissionId, String dataNeedId, CCMORequest ccmoRequest, EdaAdapter edaAdapter) {
        this.connectionId = connectionId;
        this.permissionId = permissionId;
        this.dataNeedId = dataNeedId;
        this.cmRequestId = ccmoRequest.cmRequestId();
        this.conversationId = ccmoRequest.messageId();
        this.meteringPointId = ccmoRequest.meteringPointId().orElse(null);
        this.dataSourceInformation = new EdaDataSourceInformation(ccmoRequest.dsoId());
        this.dataFrom = ccmoRequest.dataFrom();
        this.dataTo = ccmoRequest.dataTo().orElse(null);
        this.state = new AtCreatedPermissionRequestState(this, ccmoRequest, edaAdapter);
    }

    @Override
    public String permissionId() {
        return permissionId;
    }

    @Override
    public String connectionId() {
        return connectionId;
    }

    @Override
    public String dataNeedId() {
        return dataNeedId;
    }

    @Override
    public String cmRequestId() {
        return cmRequestId;
    }

    @Override
    public String conversationId() {
        return conversationId;
    }

    @Override
    public Optional<String> meteringPointId() {
        return Optional.ofNullable(meteringPointId);
    }

    @Override
    public void setMeteringPointId(String meteringPointId) {
        this.meteringPointId = meteringPointId;
    }

    @Override
    public LocalDate dataFrom() {
        return dataFrom;
    }

    @Override
    public Optional<LocalDate> dataTo() {
        return Optional.ofNullable(dataTo);
    }

    @Override
    public PermissionRequestState state() {
        return state;
    }

    @Override
    public DataSourceInformation dataSourceInformation() {
        return dataSourceInformation;
    }

    @Override
    public String stateTransitionMessage() {
        return statusTransitionMessage;
    }

    @Override
    public void setStateTransitionMessage(String message) {
        this.statusTransitionMessage = message;
    }

    @Override
    public void changeState(PermissionRequestState state) {
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EdaPermissionRequest that)) return false;
        return Objects.equals(connectionId, that.connectionId) &&
                Objects.equals(permissionId, that.permissionId) &&
                Objects.equals(cmRequestId, that.cmRequestId) &&
                Objects.equals(conversationId, that.conversationId) &&
                Objects.equals(dataFrom, that.dataFrom) &&
                Objects.equals(dataTo, that.dataTo) &&
                Objects.equals(dataNeedId, that.dataNeedId) &&
                Objects.equals(meteringPointId, that.meteringPointId) &&
                Objects.equals(state.getClass(), that.state.getClass()) &&
                Objects.equals(statusTransitionMessage, that.statusTransitionMessage) &&
                Objects.equals(dataSourceInformation, that.dataSourceInformation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectionId,
                permissionId,
                cmRequestId,
                conversationId,
                dataFrom,
                dataTo,
                dataNeedId,
                meteringPointId,
                state.getClass().hashCode(),
                statusTransitionMessage,
                dataSourceInformation.hashCode());
    }
}