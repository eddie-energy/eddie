// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.client;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import energy.eddie.regionconnector.cds.openapi.model.CarbonDataSpec200Response;
import energy.eddie.regionconnector.cds.openapi.model.Coverages200Response;
import energy.eddie.regionconnector.cds.openapi.model.Coverages200ResponseAllOfCoverageEntriesInner;
import energy.eddie.regionconnector.cds.openapi.model.OAuthAuthorizationServer200Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Component
public class CdsPublicApis {
    private static final Logger LOGGER = LoggerFactory.getLogger(CdsPublicApis.class);
    private final WebClient webClient;
    private final AsyncLoadingCache<URI, CarbonDataSpec200Response> carbonDataSpecCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .maximumSize(100)
                    .buildAsync(this::carbonDataSpecLoader);
    private final AsyncLoadingCache<URI, OAuthAuthorizationServer200Response> oauthMetadataSpecCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .maximumSize(100)
                    .buildAsync(this::oauthMetadataSpecLoader);
    private final AsyncLoadingCache<URI, List<Coverages200ResponseAllOfCoverageEntriesInner>> coverageCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .maximumSize(100)
                    .buildAsync(this::coverageLoader);


    public CdsPublicApis(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Mono<CarbonDataSpec200Response> carbonDataSpec(URI baseUrl) {
        return Mono.fromFuture(carbonDataSpecCache.get(baseUrl));
    }

    public Mono<OAuthAuthorizationServer200Response> oauthMetadataSpec(URI oauthMetadataEndpoint) {
        return Mono.fromFuture(oauthMetadataSpecCache.get(oauthMetadataEndpoint));
    }

    public Mono<List<Coverages200ResponseAllOfCoverageEntriesInner>> coverage(URI coverage) {
        return Mono.fromFuture(coverageCache.get(coverage));
    }


    private CompletableFuture<CarbonDataSpec200Response> carbonDataSpecLoader(URI baseUri, Executor executor) {
        var cdsUrl = CarbonDataSpecificationUri.create(baseUri);
        LOGGER.info("Requesting carbon data spec from {}", cdsUrl);
        return webClient.get()
                        .uri(cdsUrl)
                        .retrieve()
                        .bodyToMono(CarbonDataSpec200Response.class)
                        .toFuture();
    }

    private CompletableFuture<OAuthAuthorizationServer200Response> oauthMetadataSpecLoader(
            URI oauthMetadataEndpoint,
            Executor executor
    ) {
        return webClient.get()
                        .uri(oauthMetadataEndpoint)
                        .retrieve()
                        .bodyToMono(OAuthAuthorizationServer200Response.class)
                        .toFuture();
    }

    private CompletableFuture<List<Coverages200ResponseAllOfCoverageEntriesInner>> coverageLoader(
            URI coverageEndpoint,
            Executor executor
    ) {
        return expandedCoverages(coverageEndpoint)
                .flatMapIterable(Coverages200Response::getCoverageEntries)
                .collectList()
                .toFuture();
    }

    private Flux<Coverages200Response> expandedCoverages(URI coverage) {
        return webClient.get()
                        .uri(coverage)
                        .retrieve()
                        .bodyToMono(Coverages200Response.class)
                        .expand(res -> {
                            if (res.getNext() == null) {
                                return Mono.empty();
                            }
                            return expandedCoverages(res.getNext());
                        });
    }
}
