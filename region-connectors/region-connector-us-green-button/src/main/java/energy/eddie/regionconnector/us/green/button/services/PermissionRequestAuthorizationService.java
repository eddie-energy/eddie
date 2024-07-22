package energy.eddie.regionconnector.us.green.button.services;


import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.us.green.button.client.OAuthTokenClient;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientIdException;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientSecretException;
import energy.eddie.regionconnector.us.green.button.oauth.OAuthCallback;
import energy.eddie.regionconnector.us.green.button.oauth.dto.AccessTokenResponse;
import energy.eddie.regionconnector.us.green.button.oauth.enums.OAuthErrorResponse;
import energy.eddie.regionconnector.us.green.button.oauth.persistence.OAuthTokenDetails;
import energy.eddie.regionconnector.us.green.button.oauth.persistence.OAuthTokenRepository;
import energy.eddie.regionconnector.us.green.button.oauth.request.AccessTokenWithCodeRequest;
import energy.eddie.regionconnector.us.green.button.permission.events.UsInvalidEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsSimpleEvent;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class PermissionRequestAuthorizationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestAuthorizationService.class);
    private final UsPermissionRequestRepository permissionRequestRepository;
    private final OAuthTokenRepository oAuthTokenRepository;
    private final Outbox outbox;
    private final GreenButtonConfiguration greenButtonConfiguration;

    public PermissionRequestAuthorizationService(
            UsPermissionRequestRepository permissionRequestRepository,
            OAuthTokenRepository oAuthTokenRepository,
            Outbox outbox,
            GreenButtonConfiguration greenButtonConfiguration
    ) {
        this.permissionRequestRepository = permissionRequestRepository;
        this.oAuthTokenRepository = oAuthTokenRepository;
        this.outbox = outbox;
        this.greenButtonConfiguration = greenButtonConfiguration;
    }

    public void authorizePermissionRequest(
            OAuthCallback callback
    ) throws PermissionNotFoundException, MissingClientIdException, MissingClientSecretException {
        var permissionId = callback.state();
        var exists = permissionRequestRepository.existsById(permissionId);
        if (!exists) {
            // unknown state / permissionId => not coming / initiated by our frontend
            throw new PermissionNotFoundException(permissionId);
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
            return;
        }

        LOGGER.info("Authorization callback was successful");
        retrieveAccessToken(permissionId, callback.code().orElseThrow());
    }

    private void retrieveAccessToken(
            String permissionId, String code
    ) throws MissingClientSecretException, MissingClientIdException {
        var permissionRequest = permissionRequestRepository.getByPermissionId(permissionId);
        var companyId = permissionRequest.dataSourceInformation().permissionAdministratorId();

        String clientId;
        String clientSecret;
        if (greenButtonConfiguration.clientIds().containsKey(companyId)) {
            clientId = greenButtonConfiguration.clientIds().get(companyId);
        } else {
            throw new MissingClientIdException();
        }

        if (greenButtonConfiguration.clientSecrets().containsKey(companyId)) {
            clientSecret = greenButtonConfiguration.clientSecrets().get(companyId);
        } else {
            throw new MissingClientSecretException();
        }

        var oauthTokenClient = new OAuthTokenClient(permissionRequest.jumpOffUrl().orElseThrow(),
                                                    clientId,
                                                    clientSecret);
        var accessTokenRequest = new AccessTokenWithCodeRequest(code, greenButtonConfiguration.redirectUri());

        oauthTokenClient.accessToken(accessTokenRequest)
                        .subscribe(accessTokenResponse -> handleAccessTokenResponse(accessTokenResponse,
                                                                                    permissionId,
                                                                                    permissionRequest.scope()
                                                                                                     .orElseThrow()));
    }

    private void handleAccessTokenResponse(AccessTokenResponse accessToken, String permissionId, String originalScope) {
        if (!accessToken.getScope().equals(originalScope)) {
            // Scope was changed by the client
            outbox.commit(new UsInvalidEvent(permissionId, OAuthErrorResponse.INVALID_SCOPE));
        } else {
            // Scope is the same as requested
            outbox.commit(new UsSimpleEvent(permissionId, PermissionProcessStatus.ACCEPTED));

            var tokenIssued = Instant.now();
            var tokenDetails = new OAuthTokenDetails(permissionId,
                                                     accessToken.getAccessToken(),
                                                     tokenIssued,
                                                     tokenIssued.plusSeconds(accessToken.getExpiresIn()),
                                                     accessToken.getRefreshToken());
            oAuthTokenRepository.save(tokenDetails);
        }
    }
}
