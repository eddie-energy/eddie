package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.states.AtCreatedPermissionRequestState;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.shared.permission.requests.TimestampedPermissionRequest;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static energy.eddie.regionconnector.at.eda.utils.DateTimeConstants.AT_ZONE_ID;

public class EdaPermissionRequest extends TimestampedPermissionRequest implements AtPermissionRequest {
    private final String connectionId;
    private final String permissionId;
    private final String cmRequestId;
    private final String conversationId;
    private final ZonedDateTime start;
    @Nullable
    private final ZonedDateTime end;
    private final String dataNeedId;
    private final EdaDataSourceInformation dataSourceInformation;
    @Nullable
    private String meteringPointId;
    private PermissionRequestState state;
    private String statusTransitionMessage = "";
    @Nullable
    private String consentId;

    public EdaPermissionRequest(String connectionId, String dataNeedId, CCMORequest ccmoRequest, EdaAdapter edaAdapter, AtConfiguration atConfiguration) {
        this(connectionId, UUID.randomUUID().toString(), dataNeedId, ccmoRequest, edaAdapter, atConfiguration);
    }

    public EdaPermissionRequest(String connectionId, String permissionId, String dataNeedId, CCMORequest ccmoRequest, EdaAdapter edaAdapter, AtConfiguration atConfiguration) {
        super(AT_ZONE_ID);
        this.connectionId = connectionId;
        this.permissionId = permissionId;
        this.dataNeedId = dataNeedId;
        this.cmRequestId = ccmoRequest.cmRequestId();
        this.conversationId = ccmoRequest.messageId();
        this.meteringPointId = ccmoRequest.meteringPointId().orElse(null);
        this.dataSourceInformation = new EdaDataSourceInformation(ccmoRequest.dsoId());
        this.start = ccmoRequest.start();
        this.end = ccmoRequest.end().orElse(null);
        this.state = new AtCreatedPermissionRequestState(this, ccmoRequest, edaAdapter, atConfiguration);
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
    public Optional<String> consentId() {
        return Optional.ofNullable(consentId);
    }

    @Override
    public void setMeteringPointId(String meteringPointId) {
        this.meteringPointId = meteringPointId;
    }

    @Override
    public void setConsentId(String consentId) {
        this.consentId = consentId;
    }

    @Override
    public ZonedDateTime start() {
        return start;
    }

    @Override
    @Nullable
    public ZonedDateTime end() {
        return end;
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
                Objects.equals(start, that.start) &&
                Objects.equals(end, that.end) &&
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
                start,
                end,
                dataNeedId,
                meteringPointId,
                state.getClass().hashCode(),
                statusTransitionMessage,
                dataSourceInformation.hashCode());
    }
}