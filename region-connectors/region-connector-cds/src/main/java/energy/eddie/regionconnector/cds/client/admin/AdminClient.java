package energy.eddie.regionconnector.cds.client.admin;

import energy.eddie.regionconnector.cds.exceptions.NoTokenException;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.openapi.model.ClientEndpoint200Response;
import energy.eddie.regionconnector.cds.openapi.model.ClientEndpoint200ResponseClientsInner;
import energy.eddie.regionconnector.cds.services.oauth.OAuthService;
import energy.eddie.regionconnector.cds.services.oauth.token.CredentialsWithRefreshToken;
import energy.eddie.regionconnector.cds.services.oauth.token.CredentialsWithoutRefreshToken;
import energy.eddie.regionconnector.cds.services.oauth.token.InvalidTokenResult;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;

public class AdminClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminClient.class);
    private final WebClient webClient;
    private final CdsServer cdsServer;
    private final OAuthService oAuthService;
    @Nullable
    private CredentialsWithoutRefreshToken cachedToken = null;

    public AdminClient(
            WebClient webClient,
            CdsServer cdsServer,
            OAuthService oAuthService
    ) {
        this.cdsServer = cdsServer;
        this.oAuthService = oAuthService;
        this.webClient = webClient;
    }

    public Mono<List<ClientEndpoint200ResponseClientsInner>> clients() {
        var endpoint = cdsServer.endpoints().clientsEndpoint();
        return refreshTokenAsync()
                .flatMapMany(token -> expandedClients(token, endpoint))
                .flatMapIterable(ClientEndpoint200Response::getClients)
                .collectList();
    }

    private Flux<ClientEndpoint200Response> expandedClients(CredentialsWithoutRefreshToken token, URI next) {
        return webClient.get()
                        .uri(next)
                        .header("Authorization", "Bearer " + token.accessToken())
                        .retrieve()
                        .bodyToMono(ClientEndpoint200Response.class)
                        .expand(res -> {
                            var nextPage = res.getNext();
                            if (nextPage == null) {
                                return Mono.empty();
                            }
                            return expandedClients(token, res.getNext());
                        });
    }

    private CredentialsWithoutRefreshToken refreshToken() throws NoTokenException {
        if (cachedToken != null && cachedToken.isValid()) {
            return cachedToken;
        }
        var res = oAuthService.retrieveAccessToken(cdsServer);
        cachedToken = switch (res) {
            case CredentialsWithoutRefreshToken withoutRefreshToken -> withoutRefreshToken;
            case CredentialsWithRefreshToken(
                    String accessToken, String ignoredRefreshToken, ZonedDateTime expiresAt
            ) -> {
                LOGGER.warn("Got unexpected token response with refresh token");
                yield new CredentialsWithoutRefreshToken(accessToken, expiresAt);
            }
            case InvalidTokenResult ignored -> throw new NoTokenException();
        };
        return cachedToken;
    }

    private Mono<CredentialsWithoutRefreshToken> refreshTokenAsync() {
        return Mono.create(sink -> {
            try {
                var res = refreshToken();
                sink.success(res);
            } catch (NoTokenException e) {
                sink.error(e);
            }
        });
    }
}
