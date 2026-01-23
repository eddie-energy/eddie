// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.services.client.creation;

import energy.eddie.regionconnector.cds.client.CdsServerClientFactory;
import energy.eddie.regionconnector.cds.client.MetadataCollection;
import energy.eddie.regionconnector.cds.client.Scopes;
import energy.eddie.regionconnector.cds.config.CdsConfiguration;
import energy.eddie.regionconnector.cds.dtos.CdsServerRedirectUriUpdate;
import energy.eddie.regionconnector.cds.exceptions.CoverageNotSupportedException;
import energy.eddie.regionconnector.cds.exceptions.NoCustomerDataClientFoundException;
import energy.eddie.regionconnector.cds.exceptions.OAuthNotSupportedException;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
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
    private final CdsServerClientFactory factory;
    private final OAuthService oAuthService;
    private final CdsConfiguration cdsConfiguration;


    public CdsClientCreationService(
            CdsServerRepository repository,
            MetadataCollection metadataCollection,
            CdsServerClientFactory factory,
            OAuthService oAuthService,
            CdsConfiguration cdsConfiguration
    ) {
        this.cdsServerRepository = repository;
        this.metadataCollection = metadataCollection;
        this.factory = factory;
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
            case null -> new UnableToRegisterClientResponse("No metadata could be found");
            default -> res;
        };
    }


    private Mono<ApiClientCreationResponse> createNewAdminClientAsync(String cdsBaseUri) {
        return metadataCollection.metadata(URI.create(cdsBaseUri))
                                 .flatMap(response -> createAdminClient(cdsBaseUri, response))
                                 .onErrorReturn(WebClientResponseException.NotFound.class::isInstance,
                                                new NotACdsServerResponse())
                                 .onErrorReturn(CoverageNotSupportedException.class::isInstance,
                                                new UnsupportedFeatureResponse(
                                                        "Required coverage types are not supported"
                                                ))
                                 .onErrorReturn(OAuthNotSupportedException.class::isInstance,
                                                new UnsupportedFeatureResponse("OAuth not supported"))
                                 .onErrorReturn(NoCustomerDataClientFoundException.class::isInstance,
                                                new UnsupportedFeatureResponse("Customer data not supported"))
                                 .onErrorResume(throwable -> {
                                     LOGGER.info("Failed to create admin client for CDS server {}",
                                                 cdsBaseUri,
                                                 throwable);
                                     return Mono.just(
                                             new UnableToRegisterClientResponse(
                                                     "Failed to create admin client for CDS server because of an unknown error"
                                             )
                                     );
                                 });
    }


    private Mono<ApiClientCreationResponse> createAdminClient(
            String cdsBaseUri,
            OAuthAuthorizationServer200Response oauthMetadata
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
        var cdsServer = new CdsServer(cdsBaseUri, credentials.clientId(), credentials.clientSecret());
        var modifyingClientRequest = new CdsServerRedirectUriUpdate(List.of(cdsConfiguration.redirectUrl()));
        var cdsServerClient = factory.getTemporary(cdsServer);
        return cdsServerClient.modifyClientWithScope(Scopes.CUSTOMER_DATA_SCOPE, modifyingClientRequest)
                              .thenReturn(new CreatedCdsClientResponse(cdsServer));
    }
}
