package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestFactory;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.state.*;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class StateBuilderFactory {
    private final AuthorizationApi authorizationApi;

    public StateBuilderFactory(AuthorizationApi authorizationApi) {
        this.authorizationApi = authorizationApi;
    }

    public PermissionRequestStateBuilder create(
            EsPermissionRequest permissionRequest,
            PermissionProcessStatus status
    ) {
        return new PermissionRequestStateBuilder(permissionRequest, status);
    }

    public class PermissionRequestStateBuilder {
        private final PermissionProcessStatus status;
        private final EsPermissionRequest permissionRequest;
        private final AuthorizationRequestFactory authorizationRequestFactory = new AuthorizationRequestFactory();
        @Nullable
        private Throwable cause;
        @Nullable
        private AuthorizationRequestResponse response;

        private PermissionRequestStateBuilder(EsPermissionRequest permissionRequest, PermissionProcessStatus status) {
            this.status = status;
            this.permissionRequest = permissionRequest;
        }

        public PermissionRequestStateBuilder withCause(Throwable cause) {
            this.cause = cause;
            this.permissionRequest.setErrorMessage(cause.getMessage());
            return this;
        }

        public PermissionRequestStateBuilder withAuthorizationRequestResponse(AuthorizationRequestResponse response) {
            this.response = response;
            return this;
        }

        public PermissionRequestState build() {
            final StateBuilderFactory factory = StateBuilderFactory.this;
            return switch (status) {
                case CREATED -> new CreatedState(permissionRequest, factory);
                case VALIDATED -> new ValidatedState(permissionRequest,
                                                     factory.authorizationApi,
                                                     authorizationRequestFactory,
                                                     factory);
                case MALFORMED -> new MalformedState(permissionRequest, cause);
                case PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT ->
                        new PendingAcknowledgementState(permissionRequest, Objects.requireNonNull(response), factory);
                case UNABLE_TO_SEND -> new UnableToSendState(permissionRequest, cause);
                case SENT_TO_PERMISSION_ADMINISTRATOR ->
                        new SentToPermissionAdministratorState(permissionRequest, factory);
                case ACCEPTED -> new AcceptedState(permissionRequest, factory);
                case INVALID -> new InvalidState(permissionRequest, cause);
                case REJECTED -> new RejectedState(permissionRequest);
                case TERMINATED -> new TerminatedState(permissionRequest);
                case REVOKED -> new RevokedState(permissionRequest);
                case FULFILLED -> new FulfilledState(permissionRequest);
                case TIMED_OUT -> new TimedOutState(permissionRequest);
                default -> throw new UnsupportedOperationException();
            };
        }
    }
}
