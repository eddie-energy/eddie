package energy.eddie.regionconnector.cds.client.admin;

import energy.eddie.regionconnector.cds.client.CdsPublicApis;
import energy.eddie.regionconnector.cds.client.admin.responses.*;
import energy.eddie.regionconnector.cds.exceptions.CoverageNotSupportedException;
import energy.eddie.regionconnector.cds.exceptions.OAuthNotSupportedException;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.openapi.model.CarbonDataSpec200Response;
import energy.eddie.regionconnector.cds.openapi.model.Coverages200ResponseAllOfCoverageEntriesInner;
import energy.eddie.regionconnector.cds.openapi.model.OAuthAuthorizationServer200Response;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import energy.eddie.regionconnector.cds.services.oauth.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AdminClientFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminClientFactory.class);
    private final Map<String, AdminClient> adminClients = new ConcurrentHashMap<>();
    private final CdsPublicApis cdsPublicApis;
    private final CdsServerRepository cdsServerRepository;
    private final WebClient webClient;
    private final OAuthService oAuthService;
    private final MetadataCollection metadataCollection;

    @Autowired
    public AdminClientFactory(
            CdsPublicApis cdsPublicApis,
            CdsServerRepository cdsServerRepository,
            WebClient webClient,
            OAuthService oAuthService
    ) {
        this(
                cdsPublicApis,
                cdsServerRepository,
                webClient,
                oAuthService,
                new MetadataCollection(cdsPublicApis)
        );
    }

    AdminClientFactory(
            CdsPublicApis cdsPublicApis,
            CdsServerRepository cdsServerRepository,
            WebClient webClient,
            OAuthService oAuthService,
            MetadataCollection metadataCollection
    ) {
        this.cdsPublicApis = cdsPublicApis;
        this.cdsServerRepository = cdsServerRepository;
        this.webClient = webClient;
        this.oAuthService = oAuthService;
        this.metadataCollection = metadataCollection;
    }

    @SuppressWarnings("BlockingMethodInNonBlockingContext")
    public Mono<ApiClientCreationResponse> get(Long id) {
        var cdsServer = cdsServerRepository.getReferenceById(id);
        var cdsBaseUri = cdsServer.baseUri();
        return lookUpAdminClient(cdsBaseUri)
                .switchIfEmpty(createFromExistingCdsServer(cdsServer, cdsBaseUri));
    }

    @SuppressWarnings("BlockingMethodInNonBlockingContext")
    public Mono<ApiClientCreationResponse> getOrCreate(URL cdsServerUrl) {
        var cdsBaseUri = cdsServerUrl.getProtocol() + "://" + cdsServerUrl.getAuthority();
        LOGGER.info("Finding or creating client for {}", cdsBaseUri);
        var cdsServer = cdsServerRepository.findByBaseUri(cdsBaseUri);
        // Check if the admin client is in the hashmap
        return lookUpAdminClient(cdsBaseUri)
                // Check if the cds server already exists in the database
                .switchIfEmpty(createFromExistingCdsServer(cdsServer, cdsBaseUri))
                // Last, create a new admin client on the CDS server
                .switchIfEmpty(createNewAdminClient(cdsBaseUri));
    }

    private Mono<ApiClientCreationResponse> createNewAdminClient(String cdsBaseUri) {
        return metadataCollection.metadata(URI.create(cdsBaseUri))
                            .flatMap(tuple -> createAdminClient(
                                    cdsBaseUri,
                                    tuple.getT1(),
                                    tuple.getT2(),
                                    tuple.getT3()
                            ))
                            .onErrorReturn(WebClientResponseException.NotFound.class::isInstance,
                                           new NotACdsServerResponse())
                            .onErrorReturn(CoverageNotSupportedException.class::isInstance,
                                           new CoverageNotSupportedResponse())
                            .onErrorReturn(OAuthNotSupportedException.class::isInstance,
                                           new OAuthNotSupportedResponse());
    }

    private Mono<ApiClientCreationResponse> lookUpAdminClient(String cdsBaseUri) {
        if (adminClients.containsKey(cdsBaseUri)) {
            return Mono.just(new CreatedAdminClientResponse(adminClients.get(cdsBaseUri)));
        }
        return Mono.empty();
    }

    private Mono<CreatedAdminClientResponse> createFromExistingCdsServer(CdsServer cdsServer, String cdsBaseUri) {
        var api = new AdminClient(webClient, cdsServer, oAuthService);
        adminClients.put(cdsBaseUri, api);
        return Mono.just(new CreatedAdminClientResponse(api));
    }

    private Mono<ApiClientCreationResponse> createFromExistingCdsServer(
            Optional<CdsServer> cdsServer,
            String cdsBaseUri
    ) {
        if (cdsServer.isEmpty()) {
            return Mono.empty();
        }
        var api = new AdminClient(webClient, cdsServer.get(), oAuthService);
        adminClients.put(cdsBaseUri, api);
        return Mono.just(new CreatedAdminClientResponse(api));
    }

    private Mono<ApiClientCreationResponse> createAdminClient(
            String cdsBaseUri,
            CarbonDataSpec200Response carbonDataSpec,
            OAuthAuthorizationServer200Response oauthMetadata,
            List<Coverages200ResponseAllOfCoverageEntriesInner> coverages
    ) {
        LOGGER.info("Creating a new cds client for {}", cdsBaseUri);
        if (!oauthMetadata.getGrantTypesSupported().contains("authorization_code")) {
            return Mono.just(new AuthorizationCodeGrantTypeNotSupported());
        }
        if (!oauthMetadata.getGrantTypesSupported().contains("refresh_token")) {
            return Mono.just(new RefreshTokenGrantTypeNotSupported());
        }
        var coverageTypes = new CoverageTypes(coverages);
        var registrationEndpoint = oauthMetadata.getRegistrationEndpoint();
        LOGGER.info("Registering new CDS client at {}", registrationEndpoint);
        return cdsPublicApis.createOAuthClient(registrationEndpoint)
                            .map(creds -> new CdsServer(
                                         cdsBaseUri,
                                         carbonDataSpec.getName(),
                                         coverageTypes.toEnergyTypes(),
                                         creds.getClientId(),
                                         creds.getClientSecret(),
                                         // TODO: Get actual token endpoint and authorization endpoint
                                         buildOAuthEndpointFor(cdsBaseUri, "token"),
                                         buildOAuthEndpointFor(cdsBaseUri, "authorize"),
                                         oauthMetadata.getPushedAuthorizationRequestEndpoint().toString(),
                                         oauthMetadata.getCdsClientsApi().toString()
                                 )
                            )
                            .doOnSuccess(cdsServerRepository::save)
                            .map(res -> {
                                LOGGER.info("Created oauth credentials for {}", cdsBaseUri);
                                var cdsApiClient = new AdminClient(webClient, res, oAuthService);
                                adminClients.put(res.baseUri(), cdsApiClient);
                                return new CreatedAdminClientResponse(cdsApiClient);
                            });
    }

    private static String buildOAuthEndpointFor(String cdsBaseUri, String endpoint) {
        return UriComponentsBuilder.fromUriString(cdsBaseUri)
                                   .pathSegment("oauth", endpoint)
                                   .path("/")
                                   .build()
                                   .toString();
    }
}
