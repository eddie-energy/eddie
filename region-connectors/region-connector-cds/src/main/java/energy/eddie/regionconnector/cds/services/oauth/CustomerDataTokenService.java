package energy.eddie.regionconnector.cds.services.oauth;

import energy.eddie.regionconnector.cds.exceptions.NoTokenException;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
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

    public Mono<OAuthCredentials> getOAuthCredentialsAsync(String permissionId, CdsServer cdsServer) {
        return Mono.create(sink -> {
            try {
                sink.success(getOAuthCredentials(permissionId, cdsServer));
            } catch (NoTokenException e) {
                sink.error(e);
            }
        });
    }

    private OAuthCredentials getOAuthCredentials(String permissionId, CdsServer cdsServer) throws NoTokenException {
        var credentials = repository.getOAuthCredentialByPermissionId(permissionId);
        if (credentials.isValid()) {
            return credentials;
        }

        var res = switch (oAuthService.retrieveAccessToken(cdsServer, credentials)) {
            case CredentialsWithRefreshToken(String accessToken, String refreshToken, ZonedDateTime expiresAt) ->
                    credentials.updateAllTokens(refreshToken, accessToken, expiresAt);
            case CredentialsWithoutRefreshToken(String accessToken, ZonedDateTime expiresAt) ->
                    credentials.updateAccessToken(accessToken, expiresAt);
            case InvalidTokenResult ignored -> throw new NoTokenException();
        };
        return repository.save(res);
    }
}
