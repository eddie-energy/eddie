// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.regionconnector.shared.oauth.NoRefreshTokenException;
import energy.eddie.regionconnector.us.green.button.client.OAuthTokenClientFactory;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingApiTokenException;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientIdException;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientSecretException;
import energy.eddie.regionconnector.us.green.button.exceptions.InvalidScopesException;
import energy.eddie.regionconnector.us.green.button.oauth.dto.AccessTokenResponse;
import energy.eddie.regionconnector.us.green.button.oauth.persistence.OAuthTokenDetails;
import energy.eddie.regionconnector.us.green.button.oauth.persistence.OAuthTokenRepository;
import energy.eddie.regionconnector.us.green.button.oauth.request.AccessTokenWithCodeRequest;
import energy.eddie.regionconnector.us.green.button.oauth.request.AccessTokenWithRefreshTokenRequest;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.URI;
import java.time.Instant;

@Service
public class CredentialService {
    private final OAuthTokenRepository repository;
    private final GreenButtonConfiguration config;
    private final OAuthTokenClientFactory factory;

    public CredentialService(
            OAuthTokenRepository repository, GreenButtonConfiguration config,
            OAuthTokenClientFactory factory
    ) {
        this.repository = repository;
        this.config = config;
        this.factory = factory;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public Mono<OAuthTokenDetails> retrieveAccessToken(UsGreenButtonPermissionRequest permissionRequest) {
        var permissionId = permissionRequest.permissionId();
        //noinspection BlockingMethodInNonBlockingContext
        var creds = repository.getReferenceById(permissionId);
        if (creds.isValid()) {
            return Mono.just(creds);
        }
        if (creds.refreshToken() == null) {
            return Mono.error(new NoRefreshTokenException());
        }
        var companyId = permissionRequest.dataSourceInformation().permissionAdministratorId();
        try {
            var oAuthTokenClient = factory.create(companyId, permissionRequest.jumpOffUrl().orElseThrow());
            var res = oAuthTokenClient.accessToken(new AccessTokenWithRefreshTokenRequest(creds.refreshToken()));
            return res.map(token -> handleAccessTokenResponse(token, permissionId));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    private OAuthTokenDetails handleAccessTokenResponse(AccessTokenResponse accessToken, String permissionId) {
        var tokenIssued = Instant.now();
        var authUid = new File(URI.create(accessToken.authorizationUri()).getPath()).getName();
        var tokenDetails = new OAuthTokenDetails(
                permissionId,
                accessToken.accessToken(),
                tokenIssued,
                tokenIssued.plusSeconds(accessToken.expiresIn()),
                accessToken.refreshToken(),
                authUid
        );
        repository.save(tokenDetails);
        return tokenDetails;
    }

    public Mono<OAuthTokenDetails> retrieveAccessToken(
            UsGreenButtonPermissionRequest permissionRequest,
            String code
    ) throws MissingClientSecretException, MissingClientIdException, MissingApiTokenException {
        var companyId = permissionRequest.dataSourceInformation().permissionAdministratorId();
        var oAuthTokenClient = factory.create(companyId, permissionRequest.jumpOffUrl().orElseThrow());
        var accessTokenRequest = new AccessTokenWithCodeRequest(code, config.redirectUri());

        return oAuthTokenClient.accessToken(accessTokenRequest)
                               .flatMap(accessTokenResponse -> handleAccessTokenResponse(accessTokenResponse,
                                                                                         permissionRequest.permissionId(),
                                                                                         permissionRequest.scope()
                                                                                                          .orElseThrow()));
    }

    private Mono<OAuthTokenDetails> handleAccessTokenResponse(
            AccessTokenResponse accessToken,
            String permissionId,
            String originalScope
    ) {
        if (!accessToken.scope().equals(originalScope)) {
            // Scope was changed by the client
            return Mono.error(new InvalidScopesException());
        }
        // Scope is the same as requested
        return Mono.just(handleAccessTokenResponse(accessToken, permissionId));
    }
}
