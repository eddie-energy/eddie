package energy.eddie.regionconnector.us.green.button.services;


import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientIdException;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientSecretException;
import energy.eddie.regionconnector.us.green.button.exceptions.InvalidScopesException;
import energy.eddie.regionconnector.us.green.button.exceptions.UnauthorizedException;
import energy.eddie.regionconnector.us.green.button.oauth.OAuthCallback;
import energy.eddie.regionconnector.us.green.button.oauth.enums.OAuthErrorResponse;
import energy.eddie.regionconnector.us.green.button.permission.events.UsAcceptedEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsInvalidEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsSimpleEvent;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PermissionRequestAuthorizationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestAuthorizationService.class);
    private final UsPermissionRequestRepository permissionRequestRepository;
    private final Outbox outbox;
    private final CredentialService credentialService;

    public PermissionRequestAuthorizationService(
            UsPermissionRequestRepository permissionRequestRepository,
            Outbox outbox,
            CredentialService credentialService
    ) {
        this.permissionRequestRepository = permissionRequestRepository;
        this.outbox = outbox;
        this.credentialService = credentialService;
    }

    public void authorizePermissionRequest(
            OAuthCallback callback
    ) throws PermissionNotFoundException, MissingClientIdException, MissingClientSecretException, UnauthorizedException {
        var permissionId = callback.state();
        var pr = permissionRequestRepository.findById(permissionId);
        if (pr.isEmpty()) {
            // unknown state / permissionId => not coming / initiated by our frontend
            throw new PermissionNotFoundException(permissionId);
        }
        var permissionRequest = pr.get();
        if (permissionRequest.status() == PermissionProcessStatus.REJECTED
            || permissionRequest.status() == PermissionProcessStatus.INVALID) {
            LOGGER.info("Permission request {} was already rejected/invalidated", permissionId);
            throw new UnauthorizedException();
        }
        if (permissionRequest.status() != PermissionProcessStatus.VALIDATED) {
            LOGGER.info("Permission request {} was already accepted", permissionId);
            return;
        }

        outbox.commit(new UsSimpleEvent(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));

        // Authorization callback was not successful, thus the callback was rejected either by the user or the system
        if (!callback.isSuccessful()) {
            var oAuthErrorResponse = OAuthErrorResponse.fromError(callback.error().orElseThrow());
            if (oAuthErrorResponse.equals(OAuthErrorResponse.ACCESS_DENIED)) {
                outbox.commit(new UsSimpleEvent(permissionId, PermissionProcessStatus.REJECTED));
            } else {
                outbox.commit(new UsInvalidEvent(permissionId, oAuthErrorResponse));
            }
            throw new UnauthorizedException();
        }

        LOGGER.info("Authorization callback was successful");
        credentialService.retrieveAccessToken(pr.get(), callback.code().orElseThrow())
                         .subscribe(res -> outbox.commit(new UsAcceptedEvent(permissionId, res.authUid())),
                                    e -> handleAuthorizationError(e, permissionId));
    }

    private void handleAuthorizationError(Throwable e, String permissionId) {
        if (e instanceof InvalidScopesException) {
            outbox.commit(new UsInvalidEvent(permissionId, OAuthErrorResponse.INVALID_SCOPE));
        } else if (e instanceof MissingClientSecretException || e instanceof MissingClientIdException) {
            LOGGER.warn("Invalid client id or secret", e);
        } else {
            LOGGER.warn("Error while authorizing permission", e);
        }
    }
}
