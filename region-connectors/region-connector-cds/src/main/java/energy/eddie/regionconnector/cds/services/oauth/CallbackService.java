// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.services.oauth;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.client.CdsServerClientFactory;
import energy.eddie.regionconnector.cds.oauth.OAuthCredentials;
import energy.eddie.regionconnector.cds.permission.events.AcceptedEvent;
import energy.eddie.regionconnector.cds.permission.events.SimpleEvent;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import energy.eddie.regionconnector.cds.persistence.CdsPermissionRequestRepository;
import energy.eddie.regionconnector.cds.persistence.OAuthCredentialsRepository;
import energy.eddie.regionconnector.cds.services.oauth.authorization.AcceptedResult;
import energy.eddie.regionconnector.cds.services.oauth.authorization.AuthorizationResult;
import energy.eddie.regionconnector.cds.services.oauth.authorization.ErrorResult;
import energy.eddie.regionconnector.cds.services.oauth.authorization.UnauthorizedResult;
import energy.eddie.regionconnector.cds.services.oauth.token.CredentialsWithRefreshToken;
import energy.eddie.regionconnector.cds.services.oauth.token.CredentialsWithoutRefreshToken;
import energy.eddie.regionconnector.cds.services.oauth.token.InvalidTokenResult;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Service
public class CallbackService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CallbackService.class);
    private final Outbox outbox;
    private final CdsPermissionRequestRepository permissionRequestRepository;
    private final OAuthCredentialsRepository credentialsRepository;
    private final CdsServerClientFactory factory;

    public CallbackService(
            Outbox outbox,
            CdsPermissionRequestRepository permissionRequestRepository,
            OAuthCredentialsRepository credentialsRepository,
            CdsServerClientFactory factory
    ) {
        this.outbox = outbox;
        this.permissionRequestRepository = permissionRequestRepository;
        this.credentialsRepository = credentialsRepository;
        this.factory = factory;
    }

    public AuthorizationResult processCallback(Callback callback) throws PermissionNotFoundException {
        var pr = permissionRequestRepository.findByState(callback.state())
                                            .orElseThrow(() -> new PermissionNotFoundException(callback.state()));
        var permissionId = pr.permissionId();

        if (pr.status() == PermissionProcessStatus.REJECTED) {
            LOGGER.info("Permission request {} was already rejected", permissionId);
            return new UnauthorizedResult(permissionId, pr.status());
        }
        if (pr.status() == PermissionProcessStatus.ACCEPTED) {
            LOGGER.info("Permission request {} was already accepted", permissionId);
            return new AcceptedResult(permissionId, pr.dataNeedId());
        }
        if (pr.status() != PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR) {
            LOGGER.info("Operation cannot be done for permission request {} in state {}", permissionId, pr.status());
            return new ErrorResult(permissionId, "Wrong status of permission request " + pr.status());
        }
        var error = callback.error();
        if (error != null) {
            LOGGER.info("Permission request {} had error present {}", permissionId, error);
            if (error.equals("access_denied")) {
                outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.REJECTED));
                return new UnauthorizedResult(permissionId, PermissionProcessStatus.REJECTED);
            } else {
                return createInvalidTokenResult(permissionId, error);
            }
        }
        var code = callback.code();
        if (code == null) {
            LOGGER.info("Permission request {} has no code", permissionId);
            return createInvalidTokenResult(permissionId, "No code provided");
        }

        return getToken(code, permissionId, pr);
    }

    private AuthorizationResult getToken(
            String code,
            String permissionId,
            CdsPermissionRequest pr
    ) {
        var client = factory.get(pr);
        var result = client.retrieveCustomerCredentials(code);
        return switch (result) {
            case CredentialsWithRefreshToken(String accessToken, String refreshToken, ZonedDateTime expiresAt) -> {
                credentialsRepository.save(new OAuthCredentials(permissionId, refreshToken, accessToken, expiresAt));
                outbox.commit(new AcceptedEvent(permissionId));
                yield new AcceptedResult(permissionId, pr.dataNeedId());
            }
            case InvalidTokenResult ignored ->
                    createInvalidTokenResult(permissionId, "Could not retrieve access token");
            case CredentialsWithoutRefreshToken(String accessToken, ZonedDateTime expiresAt) -> {
                credentialsRepository.save(new OAuthCredentials(permissionId, null, accessToken, expiresAt));
                outbox.commit(new AcceptedEvent(permissionId));
                yield new AcceptedResult(permissionId, pr.dataNeedId());
            }
        };
    }

    private ErrorResult createInvalidTokenResult(String permissionId, String message) {
        outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.INVALID));
        return new ErrorResult(permissionId, message);
    }
}
