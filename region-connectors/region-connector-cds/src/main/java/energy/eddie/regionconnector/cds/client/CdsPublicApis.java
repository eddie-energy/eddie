package energy.eddie.regionconnector.cds.client;

import energy.eddie.regionconnector.cds.config.CdsConfiguration;
import energy.eddie.regionconnector.cds.openapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

@Component
public class CdsPublicApis {
    private static final Logger LOGGER = LoggerFactory.getLogger(CdsPublicApis.class);
    private static final String CDS_METADATA_ENDPOINT = "/.well-known/carbon-data-spec.json";
    private final WebClient webClient;
    private final CdsConfiguration cdsConfiguration;

    public CdsPublicApis(WebClient webClient, CdsConfiguration cdsConfiguration) {
        this.webClient = webClient;
        this.cdsConfiguration = cdsConfiguration;
    }

    public Mono<CarbonDataSpec200Response> carbonDataSpec(URI baseUrl) {
        var cdsUrl = UriComponentsBuilder.fromUri(baseUrl)
                                         .path(CDS_METADATA_ENDPOINT)
                                         .build()
                                         .toUri();
        LOGGER.info("Requesting carbon data spec from {}", cdsUrl);
        return webClient.get()
                        .uri(cdsUrl)
                        .retrieve()
                        .bodyToMono(CarbonDataSpec200Response.class);
    }

    public Mono<OAuthAuthorizationServer200Response> oauthMetadataSpec(URI oauthMetadataEndpoint) {
        return webClient.get()
                        .uri(oauthMetadataEndpoint)
                        .retrieve()
                        .bodyToMono(OAuthAuthorizationServer200Response.class);
    }

    public Mono<OAuthClientRegistration200Response> createOAuthClient(URI oauthRegistrationEndpoint) {
        var body = new OAuthClientRegistrationRequest()
                .addRedirectUrisItem(cdsConfiguration.redirectUrl())
                .scope(String.join(" ", Scopes.REQUIRED_SCOPES));
        return webClient.post()
                        .uri(oauthRegistrationEndpoint)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(OAuthClientRegistration200Response.class);
    }

    public Mono<List<Coverages200ResponseAllOfCoverageEntriesInner>> coverage(URI coverage) {
        return expandedCoverages(coverage)
                .flatMapIterable(Coverages200Response::getCoverageEntries)
                .collectList();
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
