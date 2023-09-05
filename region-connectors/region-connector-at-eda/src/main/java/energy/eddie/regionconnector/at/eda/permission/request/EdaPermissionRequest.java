package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.permission.request.states.CreatedPermissionRequestState;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;

import java.util.Objects;
import java.util.UUID;

public class EdaPermissionRequest implements AtPermissionRequest {
    private final String connectionId;
    private final String permissionId;
    private final String cmRequestId;
    private final String conversationId;
    private PermissionRequestState state;

    public EdaPermissionRequest(String connectionId, CCMORequest ccmoRequest, EdaAdapter edaAdapter) {
        this(connectionId, UUID.randomUUID().toString(), ccmoRequest, edaAdapter);
    }

    public EdaPermissionRequest(String connectionId, String permissionId, CCMORequest ccmoRequest, EdaAdapter edaAdapter) {
        this.connectionId = connectionId;
        this.permissionId = permissionId;
        this.cmRequestId = ccmoRequest.cmRequestId();
        this.conversationId = ccmoRequest.messageId();
        this.state = new CreatedPermissionRequestState(this, ccmoRequest, edaAdapter);
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
    public String cmRequestId() {
        return cmRequestId;
    }

    @Override
    public String conversationId() {
        return conversationId;
    }

    @Override
    public PermissionRequestState state() {
        return state;
    }

    @Override
    public void changeState(PermissionRequestState state) {
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AtPermissionRequest that)) return false;


        if (!Objects.equals(connectionId, that.connectionId())) return false;
        if (!Objects.equals(permissionId, that.permissionId())) return false;
        if (!Objects.equals(cmRequestId, that.cmRequestId())) return false;
        if (!Objects.equals(conversationId, that.conversationId())) return false;
        return Objects.equals(state.getClass(), that.state().getClass());
    }

    @Override
    public int hashCode() {
        int result = connectionId != null ? connectionId.hashCode() : 0;
        result = 31 * result + (permissionId != null ? permissionId.hashCode() : 0);
        result = 31 * result + (cmRequestId != null ? cmRequestId.hashCode() : 0);
        result = 31 * result + (conversationId != null ? conversationId.hashCode() : 0);
        result = 31 * result + (state != null ? state.getClass().hashCode() : 0);
        return result;
    }
}
