package energy.eddie.regionconnector.cds.services.client.creation;

import energy.eddie.regionconnector.cds.client.Scopes;
import energy.eddie.regionconnector.cds.client.admin.AdminClient;
import energy.eddie.regionconnector.cds.client.admin.AdminClientFactory;
import energy.eddie.regionconnector.cds.client.admin.MetadataCollection;
import energy.eddie.regionconnector.cds.config.CdsConfiguration;
import energy.eddie.regionconnector.cds.dtos.CdsServerRedirectUriUpdate;
import energy.eddie.regionconnector.cds.exceptions.CoverageNotSupportedException;
import energy.eddie.regionconnector.cds.exceptions.OAuthNotSupportedException;
import energy.eddie.regionconnector.cds.master.data.CdsEndpoints;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.openapi.model.CarbonDataSpec200Response;
import energy.eddie.regionconnector.cds.openapi.model.ClientEndpoint200ResponseClientsInner;
import energy.eddie.regionconnector.cds.openapi.model.Coverages200ResponseAllOfCoverageEntriesInner;
import energy.eddie.regionconnector.cds.openapi.model.OAuthAuthorizationServer200Response;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import energy.eddie.regionconnector.cds.services.client.creation.responses.*;
import energy.eddie.regionconnector.cds.services.oauth.OAuthService;
import energy.eddie.regionconnector.cds.services.oauth.client.registration.RegistrationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final AdminClientFactory adminClientFactory;
    private final OAuthService oAuthService;
    private final CdsConfiguration cdsConfiguration;


    public CdsClientCreationService(
            CdsServerRepository repository,
            AdminClientFactory adminClientFactory,
            MetadataCollection metadataCollection,
            OAuthService oAuthService,
            CdsConfiguration cdsConfiguration
    ) {
        this.cdsServerRepository = repository;
        this.metadataCollection = metadataCollection;
        this.adminClientFactory = adminClientFactory;
        this.oAuthService = oAuthService;
        this.cdsConfiguration = cdsConfiguration;
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
                                                new UnsupportedFeatureResponse(
                                                        "Required coverage types are not supported"))
                                 .onErrorReturn(OAuthNotSupportedException.class::isInstance,
                                                new UnsupportedFeatureResponse("OAuth not supported"))
                                 .onErrorReturn(NoCustomerDataClientFoundException.class::isInstance,
                                                new UnsupportedFeatureResponse("Customer data not supported"));
    }


    private Mono<ApiClientCreationResponse> createAdminClient(
            String cdsBaseUri,
            CarbonDataSpec200Response carbonDataSpec,
            OAuthAuthorizationServer200Response oauthMetadata,
            List<Coverages200ResponseAllOfCoverageEntriesInner> coverages
    ) {
        LOGGER.info("Creating a new cds client for {}", cdsBaseUri);
        if (!oauthMetadata.getGrantTypesSupported().contains("authorization_code")) {
            LOGGER.info("CDS server does not support authorization code grant type");
            return Mono.just(new UnsupportedFeatureResponse("authorization_code grant type is not supported"));
        }
        if (!oauthMetadata.getGrantTypesSupported().contains("refresh_token")) {
            LOGGER.info("CDS server does not support refresh token grant type");
            return Mono.just(new UnsupportedFeatureResponse("refresh_token grant type is not supported"));
        }
        if (oauthMetadata.getTokenEndpoint() == null) {
            LOGGER.info("CDS server does not support a token endpoint");
            return Mono.just(new UnsupportedFeatureResponse("token endpoint is required"));
        }
        var registrationEndpoint = oauthMetadata.getRegistrationEndpoint();
        LOGGER.info("Registering new CDS client at {}", registrationEndpoint);
        RegistrationResponse.Registered credentials;
        switch (oAuthService.registerClient(registrationEndpoint)) {
            case RegistrationResponse.Registered registered -> credentials = registered;
            case RegistrationResponse.RegistrationError(String description) -> {
                return Mono.just(new UnableToRegisterClientResponse(description));
            }
        }
        var coverageTypes = new CoverageTypes(coverages);
        var cdsServer = new CdsServer(
                cdsBaseUri,
                carbonDataSpec.getName(),
                coverageTypes.toCoverages(),
                credentials.clientId(),
                credentials.clientSecret(),
                new CdsEndpoints(
                        oauthMetadata.getTokenEndpoint().toString(),
                        oauthMetadata.getAuthorizationEndpoint().toString(),
                        oauthMetadata.getPushedAuthorizationRequestEndpoint().toString(),
                        oauthMetadata.getCdsClientsApi().toString(),
                        oauthMetadata.getCdsCredentialsApi().toString(),
                        oauthMetadata.getCdsCustomerdataUsagesegmentsApi().toString(),
                        oauthMetadata.getCdsCustomerdataAccountsApi().toString(),
                        oauthMetadata.getCdsCustomerdataServicecontractsApi().toString(),
                        oauthMetadata.getCdsCustomerdataServicepointsApi().toString(),
                        oauthMetadata.getCdsCustomerdataMeterdevicesApi().toString(),
                        oauthMetadata.getCdsCustomerdataBillsectionsApi().toString()
                )
        );
        var temporaryAdminClient = adminClientFactory.getTemporaryAdminClient(cdsServer);
        var modifyingClientRequest = new CdsServerRedirectUriUpdate(List.of(cdsConfiguration.redirectUrl()));
        return findCustomerDataClientId(temporaryAdminClient)
                .flatMap(customerDataClientId -> temporaryAdminClient.modifyClient(
                                 customerDataClientId, modifyingClientRequest
                         )
                )
                .flatMap(customerDataClient -> temporaryAdminClient.credentials(
                        customerDataClient.getClientId()
                ))
                .map(customerDataClients -> new CdsServer(
                        cdsServer.baseUri(),
                        cdsServer.name(),
                        cdsServer.coverages(),
                        cdsServer.adminClientId(),
                        cdsServer.adminClientSecret(),
                        cdsServer.endpoints(),
                        customerDataClients.getCredentials().getFirst().getClientId(),
                        customerDataClients.getCredentials().getFirst().getClientSecret()
                ))
                .map(CreatedCdsClientResponse::new);
    }

    private Mono<String> findCustomerDataClientId(AdminClient temporaryAdminClient) {
        return temporaryAdminClient
                .clients()
                .flatMap(this::findCustomerDataClientCredentials);
    }

    private Mono<String> findCustomerDataClientCredentials(List<ClientEndpoint200ResponseClientsInner> response) {
        for (var result : response) {
            if (result.getScope().contains(Scopes.CUSTOMER_DATA_SCOPE)) {
                var clientId = result.getClientId();
                return Mono.just(clientId);
            }
        }
        return Mono.error(new NoCustomerDataClientFoundException());
    }

    static class NoCustomerDataClientFoundException extends Exception {
    }
}
