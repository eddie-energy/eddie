package energy.eddie.regionconnector.cds.services.client.creation;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.regionconnector.cds.client.CdsPublicApis;
import energy.eddie.regionconnector.cds.client.Scopes;
import energy.eddie.regionconnector.cds.client.admin.AdminClient;
import energy.eddie.regionconnector.cds.client.admin.AdminClientFactory;
import energy.eddie.regionconnector.cds.client.admin.MetadataCollection;
import energy.eddie.regionconnector.cds.exceptions.CoverageNotSupportedException;
import energy.eddie.regionconnector.cds.exceptions.OAuthNotSupportedException;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.master.data.CdsServerBuilder;
import energy.eddie.regionconnector.cds.openapi.model.*;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import energy.eddie.regionconnector.cds.services.client.creation.responses.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CdsClientCreationServiceTest {
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
    @InjectMocks
    private CdsClientCreationService clientCreationService;

    public static Stream<Arguments> testGetOrCreate_forUnknownCdsServer_returnsGrantTypeNotSupported_whenGrantTypeNotSupported() {
        return Stream.of(
                Arguments.of("authorization_code", RefreshTokenGrantTypeNotSupported.class),
                Arguments.of("refresh_token", AuthorizationCodeGrantTypeNotSupported.class)
        );
    }

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
                        .grantTypesSupported(List.of("authorization_code", "refresh_token"))
                        .registrationEndpoint(baseUri)
                        .cdsClientsApi(baseUri)
                        .pushedAuthorizationRequestEndpoint(baseUri),
                List.of(
                        new Coverages200ResponseAllOfCoverageEntriesInner()
                                .commodityTypes(Arrays.asList(Coverages200ResponseAllOfCoverageEntriesInner.CommodityTypesEnum.values()))
                )
        );
        when(metadataCollection.metadata(baseUri))
                .thenReturn(Mono.just(metadataResponse));
        when(api.createOAuthClient(baseUri))
                .thenReturn(Mono.just(
                        new OAuthClientRegistration200Response()
                                .clientId("client-id")
                                .clientSecret("client-secret")
                                .cdsServerMetadata(baseUri)
                ));
        when(factory.get(any(CdsServer.class)))
                .thenReturn(adminClient);
        var client = new ClientEndpoint200ResponseClientsInner()
                .clientId("client-id")
                .scope(Scopes.CUSTOMER_DATA_SCOPE);
        when(adminClient.clients())
                .thenReturn(Mono.just(List.of(client)));

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
                        .grantTypesSupported(List.of("authorization_code", "refresh_token"))
                        .registrationEndpoint(baseUri)
                        .cdsClientsApi(baseUri)
                        .pushedAuthorizationRequestEndpoint(baseUri),
                List.of(
                        new Coverages200ResponseAllOfCoverageEntriesInner()
                                .commodityTypes(Arrays.asList(Coverages200ResponseAllOfCoverageEntriesInner.CommodityTypesEnum.values()))
                )
        );
        when(metadataCollection.metadata(baseUri))
                .thenReturn(Mono.just(metadataResponse));
        when(api.createOAuthClient(baseUri))
                .thenReturn(Mono.just(
                        new OAuthClientRegistration200Response()
                                .clientId("client-id")
                                .clientSecret("client-secret")
                                .cdsServerMetadata(baseUri)
                ));
        when(factory.get(any(CdsServer.class)))
                .thenReturn(adminClient);
        var client = new ClientEndpoint200ResponseClientsInner()
                .clientId("client-id")
                .scope(Scopes.CLIENT_ADMIN_SCOPE);
        when(adminClient.clients())
                .thenReturn(Mono.just(List.of(client)));

        // When
        var res = clientCreationService.createOAuthClients(baseUri.toURL());

        // Then
        assertThat(res).isInstanceOf(OAuthNotSupportedResponse.class);
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
        assertThat(res).isInstanceOf(CoverageNotSupportedResponse.class);
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
        assertThat(res).isInstanceOf(OAuthNotSupportedResponse.class);
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @MethodSource
    void testGetOrCreate_forUnknownCdsServer_returnsGrantTypeNotSupported_whenGrantTypeNotSupported(
            String grantType,
            Class<?> result
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
        assertThat(res).isInstanceOf(result);
        verify(repository, never()).save(any());
    }
}