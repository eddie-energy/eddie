package energy.eddie.regionconnector.cds.services.client.creation;

import energy.eddie.regionconnector.cds.client.CdsPublicApis;
import energy.eddie.regionconnector.cds.client.Scopes;
import energy.eddie.regionconnector.cds.client.admin.AdminClientFactory;
import energy.eddie.regionconnector.cds.client.admin.MetadataCollection;
import energy.eddie.regionconnector.cds.exceptions.CoverageNotSupportedException;
import energy.eddie.regionconnector.cds.exceptions.NoCustomerDataClientFoundException;
import energy.eddie.regionconnector.cds.exceptions.OAuthNotSupportedException;
import energy.eddie.regionconnector.cds.master.data.CdsEndpoints;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.openapi.model.CarbonDataSpec200Response;
import energy.eddie.regionconnector.cds.openapi.model.ClientEndpoint200ResponseClientsInner;
import energy.eddie.regionconnector.cds.openapi.model.Coverages200ResponseAllOfCoverageEntriesInner;
import energy.eddie.regionconnector.cds.openapi.model.OAuthAuthorizationServer200Response;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import energy.eddie.regionconnector.cds.services.client.creation.responses.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URL;
import java.util.List;

@Service
public class CdsClientCreationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CdsClientCreationService.class);
    private final CdsServerRepository cdsServerRepository;
    private final MetadataCollection metadataCollection;
    private final CdsPublicApis cdsPublicApis;
    private final AdminClientFactory adminClientFactory;

    @Autowired
    public CdsClientCreationService(
            CdsServerRepository cdsServerRepository,
            CdsPublicApis cdsPublicApis,
            AdminClientFactory adminClientFactory
    ) {
        this(
                cdsServerRepository,
                cdsPublicApis,
                adminClientFactory,
                new MetadataCollection(cdsPublicApis)
        );
    }

    CdsClientCreationService(
            CdsServerRepository repository,
            CdsPublicApis cdsPublicApis,
            AdminClientFactory adminClientFactory,
            MetadataCollection metadataCollection
    ) {
        this.cdsServerRepository = repository;
        this.cdsPublicApis = cdsPublicApis;
        this.metadataCollection = metadataCollection;
        this.adminClientFactory = adminClientFactory;
    }

    public ApiClientCreationResponse createOAuthClients(URL cdsReferenceUri) {
        var cdsBaseUri = cdsReferenceUri.getProtocol() + "://" + cdsReferenceUri.getAuthority();
        LOGGER.info("Finding or creating client for {}", cdsBaseUri);
        var cdsServer = cdsServerRepository.findByBaseUri(cdsBaseUri);
        if (cdsServer.isPresent()) {
            return new CreatedCdsClientResponse(cdsServer.get());
        }
        return createNewAdminClient(cdsBaseUri);
    }

    private ApiClientCreationResponse createNewAdminClient(String cdsBaseUri) {
        var res = createNewAdminClientAsync(cdsBaseUri).block();
        return switch (res) {
            case CreatedCdsClientResponse(CdsServer cdsServer) ->
                    new CreatedCdsClientResponse(cdsServerRepository.save(cdsServer));
            case null, default -> res;
        };
    }


    private Mono<ApiClientCreationResponse> createNewAdminClientAsync(String cdsBaseUri) {
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
                                                new OAuthNotSupportedResponse())
                                 .onErrorReturn(NoCustomerDataClientFoundException.class::isInstance,
                                                new OAuthNotSupportedResponse());
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
        if (oauthMetadata.getTokenEndpoint() == null) {
            return Mono.just(new NoTokenEndpoint());
        }
        var coverageTypes = new CoverageTypes(coverages);
        var registrationEndpoint = oauthMetadata.getRegistrationEndpoint();
        LOGGER.info("Registering new CDS client at {}", registrationEndpoint);
        return cdsPublicApis.createOAuthClient(registrationEndpoint)
                            .flatMap(creds -> {
                                         var cdsServer = new CdsServer(
                                                 cdsBaseUri,
                                                 carbonDataSpec.getName(),
                                                 coverageTypes.toEnergyTypes(),
                                                 creds.getClientId(),
                                                 creds.getClientSecret(),
                                                 new CdsEndpoints(
                                                         oauthMetadata.getTokenEndpoint().toString(),
                                                         oauthMetadata.getAuthorizationEndpoint().toString(),
                                                         oauthMetadata.getPushedAuthorizationRequestEndpoint().toString(),
                                                         oauthMetadata.getCdsClientsApi().toString()
                                                 )
                                         );
                                         return Mono.zip(Mono.just(cdsServer), getCustomerDataClientId(cdsServer));
                                     }
                            )
                            .map(res -> {
                                var cdsServer = res.getT1();
                                var customerDataClientId = res.getT2();
                                return new CdsServer(
                                        cdsServer.baseUri(),
                                        cdsServer.name(),
                                        cdsServer.coverages(),
                                        cdsServer.adminClientId(),
                                        cdsServer.adminClientSecret(),
                                        cdsServer.endpoints(),
                                        customerDataClientId
                                );
                            })
                            .map(CreatedCdsClientResponse::new);
    }

    private Mono<String> getCustomerDataClientId(CdsServer cdsServer) {
        return adminClientFactory.get(cdsServer)
                                 .clients()
                                 .flatMap(this::createCustomerDataClientCredentials);
    }

    private Mono<String> createCustomerDataClientCredentials(List<ClientEndpoint200ResponseClientsInner> response) {
        for (var result : response) {
            if (result.getScope().contains(Scopes.CUSTOMER_DATA_SCOPE)) {
                var clientId = result.getClientId();
                return Mono.just(clientId);
            }
        }
        return Mono.error(new NoCustomerDataClientFoundException());
    }
}
