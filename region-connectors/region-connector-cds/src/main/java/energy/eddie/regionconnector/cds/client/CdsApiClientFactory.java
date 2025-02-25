package energy.eddie.regionconnector.cds.client;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.regionconnector.cds.client.responses.*;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.openapi.model.CarbonDataSpec200Response;
import energy.eddie.regionconnector.cds.openapi.model.Coverages200ResponseAllOfCoverageEntriesInner;
import energy.eddie.regionconnector.cds.openapi.model.OAuthAuthorizationServer200Response;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class CdsApiClientFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(CdsApiClientFactory.class);
    private final Map<String, CdsApiClient> cdsApiClients = new ConcurrentHashMap<>();
    private final CdsPublicApis cdsPublicApis;
    private final CdsServerRepository cdsServerRepository;

    public CdsApiClientFactory(CdsPublicApis cdsPublicApis, CdsServerRepository cdsServerRepository) {
        this.cdsPublicApis = cdsPublicApis;
        this.cdsServerRepository = cdsServerRepository;
    }

    @SuppressWarnings("BlockingMethodInNonBlockingContext")
    public Mono<ApiClientCreationResponse> getCdsApiClient(URL cdsServer) {
        var cdsBaseUri = cdsServer.getProtocol() + "://" + cdsServer.getAuthority();
        LOGGER.info("Finding or creating client for {}", cdsBaseUri);
        if (cdsApiClients.containsKey(cdsBaseUri)) {
            return Mono.just(new CreatedApiClientResponse(cdsApiClients.get(cdsBaseUri)));
        }
        var cds = cdsServerRepository.findByBaseUri(cdsBaseUri);
        if (cds.isPresent()) {
            var api = new CdsApiClient();
            cdsApiClients.put(cdsBaseUri, api);
            return Mono.just(new CreatedApiClientResponse(api));
        }
        LOGGER.info("Creating a new cds client for {}", cdsBaseUri);
        return cdsPublicApis.carbonDataSpec(URI.create(cdsBaseUri))
                            .flatMap(res -> {
                                if (res.getCoverage() == null) {
                                    return Mono.just(new CoverageNotSupportedResponse());
                                }
                                if (res.getOauthMetadata() == null) {
                                    return Mono.just(new OAuthNotSupportedResponse());
                                }
                                return Mono.zip(
                                                   Mono.just(res),
                                                   cdsPublicApis.oauthMetadataSpec(res.getOauthMetadata()),
                                                   cdsPublicApis.coverage(res.getCoverage())
                                           )
                                           .flatMap(tuple -> createOAuthCredentials(
                                                   cdsBaseUri,
                                                   tuple.getT1(),
                                                   tuple.getT2(),
                                                   tuple.getT3()
                                           ));
                            })
                .onErrorReturn(throwable -> throwable instanceof WebClientResponseException.NotFound, new NotACdsServerResponse());
    }

    private Mono<ApiClientCreationResponse> createOAuthCredentials(
            String cdsBaseUri,
            CarbonDataSpec200Response carbonDataSpec,
            OAuthAuthorizationServer200Response oauthMetadata,
            List<Coverages200ResponseAllOfCoverageEntriesInner> coverages
    ) {
        if (!oauthMetadata.getGrantTypesSupported().contains("authorization_code")) {
            return Mono.just(new AuthorizationCodeGrantTypeNotSupported());
        }
        if (!oauthMetadata.getGrantTypesSupported().contains("refresh_token")) {
            return Mono.just(new RefreshTokenGrantTypeNotSupported());
        }
        var commodityTypes = toEnergyTypes(coverages);
        var registrationEndpoint = oauthMetadata.getRegistrationEndpoint();
        LOGGER.info("Registering new CDS client at {}", registrationEndpoint);
        return cdsPublicApis.createOAuthClient(registrationEndpoint)
                            .map(creds -> new CdsServer(
                                         cdsBaseUri,
                                         carbonDataSpec.getName(),
                                         commodityTypes,
                                         creds.getClientId(),
                                         creds.getClientSecret(),
                                         // TODO: Get actual token endpoint and authorization endpoint
                                         UriComponentsBuilder.fromUriString(cdsBaseUri).pathSegment("oauth", "token").build().toString(),
                                         UriComponentsBuilder.fromUriString(cdsBaseUri).pathSegment("oauth", "authorize").build().toString(),
                                         oauthMetadata.getPushedAuthorizationRequestEndpoint().toString()
                                 )
                            )
                            .doOnSuccess(cdsServerRepository::save)
                            .map(res -> {
                                LOGGER.info("Created oauth credentials for {}", cdsBaseUri);
                                var cdsApiClient = new CdsApiClient();
                                cdsApiClients.put(res.baseUri(), cdsApiClient);
                                return new CreatedApiClientResponse(cdsApiClient);
                            });
    }

    private static Set<EnergyType> toEnergyTypes(List<Coverages200ResponseAllOfCoverageEntriesInner> coverages) {
        return coverages.stream()
                        .map(Coverages200ResponseAllOfCoverageEntriesInner::getCommodityTypes)
                        .flatMap(Collection::stream)
                        .map(CdsApiClientFactory::of)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
    }

    @Nullable
    private static EnergyType of(Coverages200ResponseAllOfCoverageEntriesInner.CommodityTypesEnum commodityType) {
        return switch (commodityType) {
            case ELECTRICITY -> EnergyType.ELECTRICITY;
            case NATURAL_GAS -> EnergyType.NATURAL_GAS;
            default -> null;
        };
    }
}
