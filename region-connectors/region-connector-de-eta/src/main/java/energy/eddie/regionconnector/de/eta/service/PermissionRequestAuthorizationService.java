package energy.eddie.regionconnector.de.eta.service;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.auth.EtaAuthService;
import energy.eddie.regionconnector.de.eta.auth.AuthCallback;
import energy.eddie.regionconnector.de.eta.auth.AuthTokenResponse;
import energy.eddie.regionconnector.de.eta.permission.request.events.AcceptedEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PermissionRequestAuthorizationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestAuthorizationService.class);

    private final DePermissionRequestRepository permissionRequestRepository;
    private final Outbox outbox;
    private final EtaAuthService authService;
    private final DeEtaPlusConfiguration configuration;

    public PermissionRequestAuthorizationService(
            DePermissionRequestRepository permissionRequestRepository,
            Outbox outbox,
            EtaAuthService authService,
            DeEtaPlusConfiguration configuration
    ) {
        this.permissionRequestRepository = permissionRequestRepository;
        this.outbox = outbox;
        this.authService = authService;
        this.configuration = configuration;
    }

    public void authorizePermissionRequest(AuthCallback callback) throws PermissionNotFoundException {
        var permissionId = callback.state();
        var pr = permissionRequestRepository.findByPermissionId(permissionId);

        if (pr.isEmpty()) {
            LOGGER.warn("Permission request {} not found", permissionId);
            throw new PermissionNotFoundException(permissionId);
        }

        var permissionRequest = pr.get();

        if (permissionRequest.status() == PermissionProcessStatus.REJECTED
            || permissionRequest.status() == PermissionProcessStatus.INVALID) {
            LOGGER.info("Permission request {} was already rejected/invalidated", permissionId);
            return;
        }

        if (permissionRequest.status() != PermissionProcessStatus.VALIDATED) {
            LOGGER.info("Permission request {} was already accepted", permissionId);
            return;
        }

        outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));

        if (!callback.isSuccessful()) {
            LOGGER.info("Authorization was denied for permission request {}", permissionId);
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.REJECTED));
            return;
        }

        LOGGER.info("Authorization callback was successful for permission request {}", permissionId);

        var codeOpt = callback.code();
        if (codeOpt.isEmpty()) {
            LOGGER.error("Authorization callback code is missing for permission request {}", permissionId);
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.INVALID));
            return;
        }

        authService.exchangeCodeForToken(codeOpt.get(), configuration.auth().clientId())
                    .subscribe(
                            response -> handleTokenExchangeResponse(response, permissionId),
                            error -> handleTokenExchangeError(error, permissionId)
                    );
    }

    private void handleTokenExchangeResponse(AuthTokenResponse response, String permissionId) {
        if (response == null || !response.success()) {
            LOGGER.error("Token exchange failed for permission request {}", permissionId);
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.INVALID));
            return;
        }
        var accessToken = response.getAccessToken();
        if (accessToken == null) {
            LOGGER.error("Token exchange returned null access token for permission request {}",
                         permissionId);
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.INVALID));
            return;
        }
        LOGGER.info("Successfully obtained access token for permission request {}",
                    permissionId);
        outbox.commit(new AcceptedEvent(permissionId, accessToken, response.getRefreshToken()));
    }

    private void handleTokenExchangeError(Throwable error, String permissionId) {
        LOGGER.error("Error during token exchange for permission request {}: {}", permissionId, error.getMessage(),
                     error);
        outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.INVALID));
    }
}
