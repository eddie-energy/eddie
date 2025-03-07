package energy.eddie.regionconnector.cds.client.admin;

import energy.eddie.regionconnector.cds.dtos.CdsServerRedirectUriUpdate;
import energy.eddie.regionconnector.cds.exceptions.NoTokenException;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.openapi.model.ClientEndpoint200Response;
import energy.eddie.regionconnector.cds.openapi.model.ClientEndpoint200ResponseClientsInner;
import energy.eddie.regionconnector.cds.openapi.model.ListingCredentials200Response;
import energy.eddie.regionconnector.cds.openapi.model.RetrievingIndividualClients200Response;
import energy.eddie.regionconnector.cds.services.oauth.OAuthService;
import energy.eddie.regionconnector.cds.services.oauth.token.CredentialsWithRefreshToken;
import energy.eddie.regionconnector.cds.services.oauth.token.CredentialsWithoutRefreshToken;
import energy.eddie.regionconnector.cds.services.oauth.token.InvalidTokenResult;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Consumer;

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

    public Mono<RetrievingIndividualClients200Response> modifyClient(
            String clientId,
            CdsServerRedirectUriUpdate request
    ) {
        return refreshTokenAsync()
                .flatMap(token -> modifyClient(clientId, request, token));
    }

    public Mono<ListingCredentials200Response> credentials(String clientId) {
        return refreshTokenAsync()
                .flatMap(token -> credentials(clientId, token));
    }

    private Mono<ListingCredentials200Response> credentials(String clientId, CredentialsWithoutRefreshToken token) {
        return webClient.get()
                        .uri(
                                cdsServer.endpoints().credentialsEndpoint().toString(),
                                builder -> builder
                                        .queryParam("client_ids", clientId)
                                        .build()
                        )
                        .headers(setBearerToken(token))
                        .retrieve()
                        .bodyToMono(ListingCredentials200Response.class);
    }

    private Mono<RetrievingIndividualClients200Response> modifyClient(
            String clientId,
            CdsServerRedirectUriUpdate request,
            CredentialsWithoutRefreshToken token
    ) {
        var requestUri = UriComponentsBuilder.fromUri(cdsServer.endpoints().clientsEndpoint())
                                             .path(clientId)
                                             .build()
                                             .toUri();
        return webClient.put()
                        .uri(requestUri)
                        .headers(setBearerToken(token))
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(RetrievingIndividualClients200Response.class)
                        .doOnSuccess(body -> LOGGER.info("Modified client {}", body));
    }

    private Flux<ClientEndpoint200Response> expandedClients(CredentialsWithoutRefreshToken token, URI next) {
        return webClient.get()
                        .uri(next)
                        .headers(setBearerToken(token))
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

    private static Consumer<HttpHeaders> setBearerToken(CredentialsWithoutRefreshToken token) {
        return h -> h.setBearerAuth(token.accessToken());
    }
}
