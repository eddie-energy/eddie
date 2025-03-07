package energy.eddie.regionconnector.cds.services.client.creation;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.regionconnector.cds.client.CdsPublicApis;
import energy.eddie.regionconnector.cds.client.Scopes;
import energy.eddie.regionconnector.cds.client.admin.AdminClient;
import energy.eddie.regionconnector.cds.client.admin.AdminClientFactory;
import energy.eddie.regionconnector.cds.client.admin.MetadataCollection;
import energy.eddie.regionconnector.cds.config.CdsConfiguration;
import energy.eddie.regionconnector.cds.exceptions.CoverageNotSupportedException;
import energy.eddie.regionconnector.cds.exceptions.OAuthNotSupportedException;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.master.data.CdsServerBuilder;
import energy.eddie.regionconnector.cds.openapi.model.*;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import energy.eddie.regionconnector.cds.services.client.creation.responses.CreatedCdsClientResponse;
import energy.eddie.regionconnector.cds.services.client.creation.responses.NotACdsServerResponse;
import energy.eddie.regionconnector.cds.services.client.creation.responses.UnableToRegisterClientResponse;
import energy.eddie.regionconnector.cds.services.client.creation.responses.UnsupportedFeatureResponse;
import energy.eddie.regionconnector.cds.services.oauth.OAuthService;
import energy.eddie.regionconnector.cds.services.oauth.client.registration.RegistrationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CdsClientCreationServiceTest {
    @Spy
    @SuppressWarnings("unused")
    private final CdsConfiguration ignored = new CdsConfiguration(URI.create("http://localhost"), "EDDIE");
    @Mock
    private CdsServerRepository repository;
    @Mock
    private MetadataCollection metadataCollection;
    @Mock
    private CdsPublicApis api;
    @Mock
    private AdminClientFactory factory;
    @Mock
    private AdminClient adminClient;
    @Mock
    private OAuthService oAuthService;
    @InjectMocks
    private CdsClientCreationService clientCreationService;

    @Test
    void testGetOrCreate_forKnownCdsServer_returnsClient() throws MalformedURLException {
        // Given
        var cdsServer = new CdsServerBuilder()
                .setBaseUri("http://localhost:8080")
                .build();
        when(repository.findByBaseUri("http://localhost:8080"))
                .thenReturn(Optional.of(cdsServer));

        // When
        var res = clientCreationService.createOAuthClients(URI.create("http://localhost:8080").toURL());

        // Then
        assertThat(res).isInstanceOf(CreatedCdsClientResponse.class);
        verify(api, never()).carbonDataSpec(any());
    }


    @Test
    void testGetOrCreate_forUnknownCdsServer_createsAndReturnsClient() throws MalformedURLException {
        // Given
        var baseUrl = "http://localhost:8080";
        var baseUri = URI.create(baseUrl);
        when(repository.findByBaseUri(baseUrl)).thenReturn(Optional.empty());
        var metadataResponse = Tuples.of(
                new CarbonDataSpec200Response()
                        .name("CDS Server")
                        .coverage(baseUri)
                        .oauthMetadata(baseUri)
                        .capabilities(List.of("coverage", "oauth")),
                new OAuthAuthorizationServer200Response()
                        .tokenEndpoint(baseUri)
                        .authorizationEndpoint(baseUri)
                        .grantTypesSupported(List.of("authorization_code", "refresh_token"))
                        .registrationEndpoint(baseUri)
                        .cdsClientsApi(baseUri)
                        .cdsCredentialsApi(baseUri)
                        .pushedAuthorizationRequestEndpoint(baseUri),
                List.of(
                        new Coverages200ResponseAllOfCoverageEntriesInner()
                                .commodityTypes(Arrays.asList(Coverages200ResponseAllOfCoverageEntriesInner.CommodityTypesEnum.values()))
                )
        );
        when(metadataCollection.metadata(baseUri))
                .thenReturn(Mono.just(metadataResponse));
        when(oAuthService.registerClient(baseUri))
                .thenReturn(new RegistrationResponse.Registered(
                        "client-id",
                        "client-secret"
                ));
        when(factory.getTemporaryAdminClient(any(CdsServer.class)))
                .thenReturn(adminClient);
        var client = new ClientEndpoint200ResponseClientsInner()
                .clientId("client-id")
                .scope(Scopes.CUSTOMER_DATA_SCOPE);
        when(adminClient.clients())
                .thenReturn(Mono.just(List.of(client)));
        when(adminClient.modifyClient(eq("client-id"), any()))
                .thenReturn(Mono.just(new RetrievingIndividualClients200Response().clientId("client-id")));
        var credentialResponse = new ListingCredentials200Response()
                .addCredentialsItem(new ListingCredentials200ResponseCredentialsInner()
                                            .clientId("client-id")
                                            .clientSecret("client-secret"));
        when(adminClient.credentials("client-id"))
                .thenReturn(Mono.just(credentialResponse));

        // When
        var res = clientCreationService.createOAuthClients(baseUri.toURL());

        // Then
        assertThat(res).isInstanceOf(CreatedCdsClientResponse.class);
        verify(repository).save(assertArg(cds -> assertAll(
                () -> assertEquals(baseUrl, cds.baseUri()),
                () -> assertEquals("CDS Server", cds.name()),
                () -> assertEquals(Set.of(EnergyType.ELECTRICITY, EnergyType.NATURAL_GAS), cds.coverages()),
                () -> assertEquals("client-id", cds.adminClientId()),
                () -> assertEquals("client-secret", cds.adminClientSecret())
        )));
    }


    @Test
    void testGetOrCreate_forCdsServerWithoutCustomerDataClient_returnsOAuthNotSupportedResponse() throws MalformedURLException {
        // Given
        var baseUrl = "http://localhost:8080";
        var baseUri = URI.create(baseUrl);
        when(repository.findByBaseUri(baseUrl)).thenReturn(Optional.empty());
        var metadataResponse = Tuples.of(
                new CarbonDataSpec200Response()
                        .name("CDS Server")
                        .coverage(baseUri)
                        .oauthMetadata(baseUri)
                        .capabilities(List.of("coverage", "oauth")),
                new OAuthAuthorizationServer200Response()
                        .authorizationEndpoint(baseUri)
                        .tokenEndpoint(baseUri)
                        .grantTypesSupported(List.of("authorization_code", "refresh_token"))
                        .registrationEndpoint(baseUri)
                        .cdsClientsApi(baseUri)
                        .cdsCredentialsApi(baseUri)
                        .pushedAuthorizationRequestEndpoint(baseUri),
                List.of(
                        new Coverages200ResponseAllOfCoverageEntriesInner()
                                .commodityTypes(Arrays.asList(Coverages200ResponseAllOfCoverageEntriesInner.CommodityTypesEnum.values()))
                )
        );
        when(metadataCollection.metadata(baseUri))
                .thenReturn(Mono.just(metadataResponse));
        when(oAuthService.registerClient(baseUri))
                .thenReturn(new RegistrationResponse.Registered(
                        "client-id",
                        "client-secret"
                ));
        when(factory.getTemporaryAdminClient(any(CdsServer.class)))
                .thenReturn(adminClient);
        var client = new ClientEndpoint200ResponseClientsInner()
                .clientId("client-id")
                .scope(Scopes.CLIENT_ADMIN_SCOPE);
        when(adminClient.clients())
                .thenReturn(Mono.just(List.of(client)));

        // When
        var res = clientCreationService.createOAuthClients(baseUri.toURL());

        // Then
        var unsupported = assertInstanceOf(UnsupportedFeatureResponse.class, res);
        assertEquals("Customer data not supported", unsupported.message());
    }

    @Test
    void testGetOrCreate_forUnknownCdsServer_returnsCoverageNotSupported_forCDSServerThatDoesNotSupportCoverage() throws MalformedURLException {
        // Given
        var baseUrl = "http://localhost:8080";
        var baseUri = URI.create(baseUrl);
        when(repository.findByBaseUri(baseUrl)).thenReturn(Optional.empty());
        when(metadataCollection.metadata(baseUri))
                .thenReturn(Mono.error(new CoverageNotSupportedException()));

        // When
        var res = clientCreationService.createOAuthClients(baseUri.toURL());

        // Then
        var unsupportedFeature = assertInstanceOf(UnsupportedFeatureResponse.class, res);
        assertEquals("Required coverage types are not supported", unsupportedFeature.message());
    }

    @Test
    void testGetOrCreate_forUnknownCdsServer_returnsNotACdsServer_forInvalidCdsServer() throws MalformedURLException {
        // Given
        var baseUrl = "http://localhost:8080";
        var baseUri = URI.create(baseUrl);
        when(repository.findByBaseUri(baseUrl)).thenReturn(Optional.empty());
        when(metadataCollection.metadata(baseUri))
                .thenReturn(Mono.error(WebClientResponseException.create(404, "text", null, null, null)));

        // When
        var res = clientCreationService.createOAuthClients(baseUri.toURL());

        // Then
        assertThat(res).isInstanceOf(NotACdsServerResponse.class);
    }

    @Test
    void testGetOrCreate_forUnknownCdsServer_returnsOAuthNotSupported_forCDSServerThatDoesNotSupportOAuth() throws MalformedURLException {
        // Given
        var baseUrl = "http://localhost:8080";
        var baseUri = URI.create(baseUrl);
        when(repository.findByBaseUri(baseUrl)).thenReturn(Optional.empty());
        when(metadataCollection.metadata(baseUri))
                .thenReturn(Mono.error(new OAuthNotSupportedException()));

        // When
        var res = clientCreationService.createOAuthClients(baseUri.toURL());

        // Then
        var unsupportedFeature = assertInstanceOf(UnsupportedFeatureResponse.class, res);
        assertEquals("OAuth not supported", unsupportedFeature.message());
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"authorization_code", "refresh_token"})
    void testGetOrCreate_forUnknownCdsServer_returnsUnsupportedFeatureResponse_whenGrantTypeNotSupported(
            String grantType
    ) throws MalformedURLException {
        // Given
        var baseUrl = "http://localhost:8080";
        var baseUri = URI.create(baseUrl);
        when(repository.findByBaseUri(baseUrl)).thenReturn(Optional.empty());
        var metadata = Tuples.of(
                new CarbonDataSpec200Response()
                        .name("CDS Server")
                        .coverage(baseUri)
                        .oauthMetadata(baseUri)
                        .capabilities(List.of("coverage", "oauth")),
                new OAuthAuthorizationServer200Response()
                        .tokenEndpoint(baseUri)
                        .grantTypesSupported(List.of(grantType))
                        .registrationEndpoint(baseUri),
                List.of(
                        new Coverages200ResponseAllOfCoverageEntriesInner()
                                .commodityTypes(Arrays.asList(
                                        Coverages200ResponseAllOfCoverageEntriesInner.CommodityTypesEnum.values()))
                )
        );
        when(metadataCollection.metadata(baseUri))
                .thenReturn(Mono.just(metadata));

        // When
        var res = clientCreationService.createOAuthClients(baseUri.toURL());

        // Then
        assertThat(res).isInstanceOf(UnsupportedFeatureResponse.class);
        verify(repository, never()).save(any());
    }

    @Test
    void testGetOrCreate_forUnknownCdsServer_returnsNoTokenEndpoint_whenNoTokenEndpointPresent() throws MalformedURLException {
        // Given
        var baseUrl = "http://localhost:8080";
        var baseUri = URI.create(baseUrl);
        when(repository.findByBaseUri(baseUrl)).thenReturn(Optional.empty());
        var metadata = Tuples.of(
                new CarbonDataSpec200Response()
                        .name("CDS Server")
                        .coverage(baseUri)
                        .oauthMetadata(baseUri)
                        .capabilities(List.of("coverage", "oauth")),
                new OAuthAuthorizationServer200Response()
                        .authorizationEndpoint(baseUri)
                        .grantTypesSupported(List.of("authorization_code", "refresh_token"))
                        .registrationEndpoint(baseUri),
                List.of(
                        new Coverages200ResponseAllOfCoverageEntriesInner()
                                .commodityTypes(Arrays.asList(
                                        Coverages200ResponseAllOfCoverageEntriesInner.CommodityTypesEnum.values()))
                )
        );
        when(metadataCollection.metadata(baseUri))
                .thenReturn(Mono.just(metadata));

        // When
        var res = clientCreationService.createOAuthClients(baseUri.toURL());

        // Then
        var unsupported = assertInstanceOf(UnsupportedFeatureResponse.class, res);
        assertEquals("token endpoint is required", unsupported.message());
    }

    @Test
    void testGetOrCreate_forUnknownCdsServer_returnsUnableToRegisterClientResponse_whenClientCouldNotBeRegistered() throws MalformedURLException {
        // Given
        var baseUrl = "http://localhost:8080";
        var baseUri = URI.create(baseUrl);
        when(repository.findByBaseUri(baseUrl)).thenReturn(Optional.empty());
        var metadata = Tuples.of(
                new CarbonDataSpec200Response()
                        .name("CDS Server")
                        .coverage(baseUri)
                        .oauthMetadata(baseUri)
                        .capabilities(List.of("coverage", "oauth")),
                new OAuthAuthorizationServer200Response()
                        .tokenEndpoint(baseUri)
                        .authorizationEndpoint(baseUri)
                        .grantTypesSupported(List.of("authorization_code", "refresh_token"))
                        .registrationEndpoint(baseUri),
                List.of(
                        new Coverages200ResponseAllOfCoverageEntriesInner()
                                .commodityTypes(Arrays.asList(
                                        Coverages200ResponseAllOfCoverageEntriesInner.CommodityTypesEnum.values()))
                )
        );
        when(metadataCollection.metadata(baseUri))
                .thenReturn(Mono.just(metadata));
        when(oAuthService.registerClient(baseUri))
                .thenReturn(new RegistrationResponse.RegistrationError("bla"));

        // When
        var res = clientCreationService.createOAuthClients(baseUri.toURL());

        // Then
        var response = assertInstanceOf(UnableToRegisterClientResponse.class, res);
        assertEquals("bla", response.message());
    }
}