// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.client.admin;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import energy.eddie.regionconnector.cds.dtos.CdsServerRedirectUriUpdate;
import energy.eddie.regionconnector.cds.openapi.model.*;
import energy.eddie.regionconnector.cds.services.oauth.token.CredentialsWithoutRefreshToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
public class AdminClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminClient.class);
    private final WebClient webClient;
    private final AsyncLoadingCache<Tuple2<URI, CredentialsWithoutRefreshToken>, List<ClientEndpoint200ResponseClientsInner>> clientsCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .maximumSize(100)
                    .buildAsync(this::clientLoader);
    private final AsyncLoadingCache<Tuple3<String, URI, CredentialsWithoutRefreshToken>, ListingCredentials200Response> credentialsCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .maximumSize(100)
                    .buildAsync(AdminClient.this::credentialsLoader);
    private final AsyncLoadingCache<Tuple2<URI, CredentialsWithoutRefreshToken>, CarbonDataSpec200Response> carbonDataSpecCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .maximumSize(100)
                    .buildAsync(AdminClient.this::carbonDataSpecLoader);
    private final AsyncLoadingCache<Tuple2<URI, CredentialsWithoutRefreshToken>, OAuthAuthorizationServer200Response> oauthMetadataSpecCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .maximumSize(100)
                    .buildAsync(AdminClient.this::oauthMetadataSpecLoader);

    public AdminClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Mono<List<ClientEndpoint200ResponseClientsInner>> clients(
            URI clientEndpoint,
            CredentialsWithoutRefreshToken token
    ) {
        var tuple = Tuples.of(clientEndpoint, token);
        return Mono.fromFuture(clientsCache.get(tuple));
    }

    public Mono<RetrievingIndividualClients200Response> modifyClient(
            CdsServerRedirectUriUpdate request,
            URI clientModificationEndpoint,
            CredentialsWithoutRefreshToken token
    ) {
        return webClient.put()
                        .uri(clientModificationEndpoint)
                        .headers(setBearerToken(token))
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(RetrievingIndividualClients200Response.class)
                        .doOnError(error -> LOGGER.warn(error.getMessage(), error));
    }

    public Mono<ListingCredentials200Response> credentials(
            String clientId,
            URI credentialsUri,
            CredentialsWithoutRefreshToken token
    ) {
        return Mono.fromFuture(credentialsCache.get(Tuples.of(clientId, credentialsUri, token)));
    }

    public Mono<CarbonDataSpec200Response> carbonDataSpec(
            URI carbonDataSpecEndpoint,
            CredentialsWithoutRefreshToken token
    ) {
        return Mono.fromFuture(carbonDataSpecCache.get(Tuples.of(carbonDataSpecEndpoint, token)));
    }

    public Mono<OAuthAuthorizationServer200Response> oauthMetadataSpec(
            URI oauthMetadataEndpoint,
            CredentialsWithoutRefreshToken token
    ) {
        return Mono.fromFuture(oauthMetadataSpecCache.get(Tuples.of(oauthMetadataEndpoint, token)));
    }

    private CompletableFuture<OAuthAuthorizationServer200Response> oauthMetadataSpecLoader(
            Tuple2<URI, CredentialsWithoutRefreshToken> tuple,
            Executor executor
    ) {
        return webClient.get()
                        .uri(tuple.getT1())
                        .headers(setBearerToken(tuple.getT2()))
                        .retrieve()
                        .bodyToMono(OAuthAuthorizationServer200Response.class)
                        .toFuture();
    }

    private CompletableFuture<CarbonDataSpec200Response> carbonDataSpecLoader(
            Tuple2<URI, CredentialsWithoutRefreshToken> tuple,
            Executor executor
    ) {
        return webClient.get()
                        .uri(tuple.getT1())
                        .headers(setBearerToken(tuple.getT2()))
                        .retrieve()
                        .bodyToMono(CarbonDataSpec200Response.class)
                        .toFuture();
    }

    private CompletableFuture<ListingCredentials200Response> credentialsLoader(
            Tuple3<String, URI, CredentialsWithoutRefreshToken> tuple,
            Executor executor
    ) {
        return webClient.get()
                        .uri(tuple.getT2().toString(),
                             builder -> builder
                                     .queryParam("client_ids", tuple.getT1())
                                     .build())
                        .headers(setBearerToken(tuple.getT3()))
                        .retrieve()
                        .bodyToMono(ListingCredentials200Response.class)
                        .toFuture();
    }

    private CompletableFuture<List<ClientEndpoint200ResponseClientsInner>> clientLoader(
            Tuple2<URI, CredentialsWithoutRefreshToken> tuple,
            Executor executor
    ) {
        return expandedClients(tuple.getT2(), tuple.getT1())
                .flatMapIterable(ClientEndpoint200Response::getClients)
                .collectList()
                .toFuture();
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

    private static Consumer<HttpHeaders> setBearerToken(CredentialsWithoutRefreshToken token) {
        return h -> h.setBearerAuth(token.accessToken());
    }
}
