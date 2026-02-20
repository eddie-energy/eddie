// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.service;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.oauth.EtaOAuthService;
import energy.eddie.regionconnector.de.eta.oauth.OAuthCallback;
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
    private final EtaOAuthService oauthService;
    private final DeEtaPlusConfiguration configuration;

    public PermissionRequestAuthorizationService(
            DePermissionRequestRepository permissionRequestRepository,
            Outbox outbox,
            EtaOAuthService oauthService,
            DeEtaPlusConfiguration configuration) {
        this.permissionRequestRepository = permissionRequestRepository;
        this.outbox = outbox;
        this.oauthService = oauthService;
        this.configuration = configuration;
    }

    public void authorizePermissionRequest(OAuthCallback callback) throws PermissionNotFoundException {
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
        oauthService.exchangeCodeForToken(callback.code().orElseThrow(), configuration.oauth().clientId())
                .subscribe(
                        response -> {
                            if (response != null && response.success()) {
                                String accessToken = response.getAccessToken();
                                if (accessToken == null) {
                                    LOGGER.error("Token exchange returned null access token for permission request {}",
                                            permissionId);
                                    outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.INVALID));
                                    return;
                                }
                                LOGGER.info("Successfully obtained access token for permission request {}",
                                        permissionId);
                                outbox.commit(new AcceptedEvent(permissionId, accessToken));
                            } else {
                                LOGGER.error("Token exchange failed for permission request {}", permissionId);
                                outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.INVALID));
                            }
                        },
                        error -> {
                            LOGGER.error("Error during token exchange for permission request " + permissionId, error);
                            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.INVALID));
                        });
    }
}
