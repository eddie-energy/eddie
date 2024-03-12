package energy.eddie.regionconnector.dk.energinet.permission.request;

import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.states.*;
import jakarta.annotation.Nullable;

public class StateBuilderFactory {
    public PermissionRequestStateBuilder create(DkEnerginetCustomerPermissionRequest permissionRequest,
                                                PermissionProcessStatus status) {
        return new PermissionRequestStateBuilder(permissionRequest, status);
    }

    public class PermissionRequestStateBuilder {
        private final PermissionProcessStatus status;
        private final DkEnerginetCustomerPermissionRequest permissionRequest;
        @Nullable
        private Throwable cause;

        private PermissionRequestStateBuilder(DkEnerginetCustomerPermissionRequest permissionRequest, PermissionProcessStatus status) {
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
                case CREATED -> new EnerginetCustomerCreatedState(permissionRequest, factory);
                case VALIDATED -> new EnerginetCustomerValidatedState(permissionRequest, factory);
                case MALFORMED -> new EnerginetCustomerMalformedState(permissionRequest, cause);
                case PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT ->
                        new EnerginetCustomerPendingAcknowledgmentState(permissionRequest, factory);
                case UNABLE_TO_SEND -> new EnerginetCustomerUnableToSendState(permissionRequest, cause);
                case SENT_TO_PERMISSION_ADMINISTRATOR ->
                        new EnerginetCustomerSentToPermissionAdministratorState(permissionRequest, factory);
                case ACCEPTED -> new EnerginetCustomerAcceptedState(permissionRequest, factory);
                case INVALID -> new EnerginetCustomerInvalidState(permissionRequest);
                case REJECTED -> new EnerginetCustomerRejectedState(permissionRequest);
                case TERMINATED -> new EnerginetCustomerTerminatedState(permissionRequest);
                case REVOKED -> new EnerginetCustomerRevokedState(permissionRequest);
                case FULFILLED -> new EnerginetCustomerFulfilledState(permissionRequest);
                default -> throw new UnsupportedOperationException();
            };
        }
    }
}
