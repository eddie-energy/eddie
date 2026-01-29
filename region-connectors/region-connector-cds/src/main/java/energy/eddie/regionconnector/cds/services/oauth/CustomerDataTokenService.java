// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.services.oauth;

import energy.eddie.regionconnector.cds.client.CustomerDataClientCredentials;
import energy.eddie.regionconnector.cds.exceptions.NoTokenException;
import energy.eddie.regionconnector.cds.oauth.OAuthCredentials;
import energy.eddie.regionconnector.cds.persistence.OAuthCredentialsRepository;
import energy.eddie.regionconnector.cds.services.oauth.token.CredentialsWithRefreshToken;
import energy.eddie.regionconnector.cds.services.oauth.token.CredentialsWithoutRefreshToken;
import energy.eddie.regionconnector.cds.services.oauth.token.InvalidTokenResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;

@Service
public class CustomerDataTokenService {
    private final OAuthService oAuthService;
    private final OAuthCredentialsRepository repository;

    public CustomerDataTokenService(OAuthService oAuthService, OAuthCredentialsRepository repository) {
        this.oAuthService = oAuthService;
        this.repository = repository;
    }

    public Mono<OAuthCredentials> getOAuthCredentialsAsync(
            String permissionId,
            CustomerDataClientCredentials credentials
    ) {
        return Mono.create(sink -> {
            try {
                sink.success(getOAuthCredentials(permissionId, credentials));
            } catch (NoTokenException e) {
                sink.error(e);
            }
        });
    }

    private OAuthCredentials getOAuthCredentials(
            String permissionId,
            CustomerDataClientCredentials customerDataClientCredentials
    ) throws NoTokenException {
        var credentials = repository.getOAuthCredentialByPermissionId(permissionId);
        if (credentials.isValid()) {
            return credentials;
        }

        var res = switch (oAuthService.retrieveAccessToken(credentials,
                                                           customerDataClientCredentials.clientId(),
                                                           customerDataClientCredentials.clientSecret(),
                                                           customerDataClientCredentials.tokenEndpoint())) {
            case CredentialsWithRefreshToken(String accessToken, String refreshToken, ZonedDateTime expiresAt) ->
                    credentials.updateAllTokens(refreshToken, accessToken, expiresAt);
            case CredentialsWithoutRefreshToken(String accessToken, ZonedDateTime expiresAt) ->
                    credentials.updateAccessToken(accessToken, expiresAt);
            case InvalidTokenResult ignored -> throw new NoTokenException();
        };
        return repository.save(res);
    }
}
