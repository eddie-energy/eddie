package energy.eddie.regionconnector.at.eda.permission.request;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.CMRequest;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.states.*;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import static java.util.Objects.requireNonNull;

@Service
public class StateBuilderFactory {
    private final AtConfiguration configuration;
    private final EdaAdapter edaAdapter;


    public StateBuilderFactory(AtConfiguration configuration, EdaAdapter edaAdapter) {
        this.configuration = configuration;
        this.edaAdapter = edaAdapter;
    }

    public PermissionRequestStateBuilder create(AtPermissionRequest permissionRequest,
                                                PermissionProcessStatus status) {
        return new PermissionRequestStateBuilder(permissionRequest, status);
    }

    public class PermissionRequestStateBuilder {
        private final PermissionProcessStatus status;
        private final AtPermissionRequest permissionRequest;
        @Nullable
        private CCMORequest ccmoRequest;
        @Nullable
        private CMRequest cmRequest;
        @Nullable
        private Throwable cause;

        private PermissionRequestStateBuilder(AtPermissionRequest permissionRequest, PermissionProcessStatus status) {
            this.status = status;
            this.permissionRequest = permissionRequest;
        }

        public PermissionRequestStateBuilder withCCMORequest(CCMORequest ccmoRequest) {
            this.ccmoRequest = ccmoRequest;
            return this;
        }

        public PermissionRequestStateBuilder withCMRequest(CMRequest cmRequest) {
            this.cmRequest = cmRequest;
            return this;
        }

        public PermissionRequestStateBuilder withCause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        public PermissionRequestState build() {
            final StateBuilderFactory factory = StateBuilderFactory.this;
            return switch (status) {
                case CREATED ->
                        new AtCreatedPermissionRequestState(permissionRequest, requireNonNull(ccmoRequest), factory);
                case VALIDATED ->
                        new AtValidatedPermissionRequestState(permissionRequest, requireNonNull(cmRequest), factory.edaAdapter, factory);
                case MALFORMED -> new AtMalformedPermissionRequestState(permissionRequest, requireNonNull(cause));
                case PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT ->
                        new AtPendingAcknowledgmentPermissionRequestState(permissionRequest, factory);
                case UNABLE_TO_SEND ->
                        new AtUnableToSendPermissionRequestState(permissionRequest, requireNonNull(cause));
                case SENT_TO_PERMISSION_ADMINISTRATOR ->
                        new AtSentToPermissionAdministratorPermissionRequestState(permissionRequest, factory);
                case ACCEPTED ->
                        new AtAcceptedPermissionRequestState(permissionRequest, edaAdapter, configuration, factory);
                case INVALID -> new AtInvalidPermissionRequestState(permissionRequest);
                case REJECTED -> new AtRejectedPermissionRequestState(permissionRequest);
                case TERMINATED -> new AtTerminatedPermissionRequestState(permissionRequest);
                case REVOKED -> new AtRevokedPermissionRequestState(permissionRequest);
                default -> throw new UnsupportedOperationException();
            };
        }
    }
}
