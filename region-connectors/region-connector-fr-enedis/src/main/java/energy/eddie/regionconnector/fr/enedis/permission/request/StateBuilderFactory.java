package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.states.*;
import jakarta.annotation.Nullable;

public class StateBuilderFactory {
    public PermissionRequestStateBuilder create(FrEnedisPermissionRequest permissionRequest,
                                                PermissionProcessStatus status) {
        return new PermissionRequestStateBuilder(permissionRequest, status);
    }

    public class PermissionRequestStateBuilder {
        private final PermissionProcessStatus status;
        private final FrEnedisPermissionRequest permissionRequest;
        @Nullable
        private Throwable cause;

        private PermissionRequestStateBuilder(FrEnedisPermissionRequest permissionRequest, PermissionProcessStatus status) {
            this.status = status;
            this.permissionRequest = permissionRequest;
        }

        public PermissionRequestStateBuilder withCause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        public PermissionRequestState build() {
            final StateBuilderFactory factory = StateBuilderFactory.this;
            return switch (status) {
                case CREATED -> new FrEnedisCreatedState(permissionRequest, factory);
                case VALIDATED -> new FrEnedisValidatedState(permissionRequest, factory);
                case MALFORMED -> new FrEnedisMalformedState(permissionRequest, cause);
                case PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT ->
                        new FrEnedisPendingAcknowledgmentState(permissionRequest, factory);
                case UNABLE_TO_SEND -> new FrEnedisUnableToSendState(permissionRequest, cause);
                case SENT_TO_PERMISSION_ADMINISTRATOR ->
                        new FrEnedisSentToPermissionAdministratorState(permissionRequest, factory);
                case ACCEPTED -> new FrEnedisAcceptedState(permissionRequest, factory);
                case INVALID -> new FrEnedisInvalidState(permissionRequest);
                case REJECTED -> new FrEnedisRejectedState(permissionRequest);
                case TERMINATED -> new FrEnedisTerminatedState(permissionRequest);
                case REVOKED -> new FrEnedisRevokedState(permissionRequest);
                case FULFILLED -> new FrEnedisFulfilledState(permissionRequest);
                default -> throw new UnsupportedOperationException();
            };
        }
    }
}
