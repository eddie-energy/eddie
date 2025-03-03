package energy.eddie.regionconnector.cds.client.admin;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.regionconnector.cds.client.CdsPublicApis;
import energy.eddie.regionconnector.cds.client.admin.responses.*;
import energy.eddie.regionconnector.cds.exceptions.CoverageNotSupportedException;
import energy.eddie.regionconnector.cds.exceptions.OAuthNotSupportedException;
import energy.eddie.regionconnector.cds.master.data.CdsServerBuilder;
import energy.eddie.regionconnector.cds.openapi.model.CarbonDataSpec200Response;
import energy.eddie.regionconnector.cds.openapi.model.Coverages200ResponseAllOfCoverageEntriesInner;
import energy.eddie.regionconnector.cds.openapi.model.OAuthAuthorizationServer200Response;
import energy.eddie.regionconnector.cds.openapi.model.OAuthClientRegistration200Response;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import energy.eddie.regionconnector.cds.services.oauth.OAuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminClientFactoryTest {
    @Mock
    private CdsPublicApis api;
    @Mock
    private CdsServerRepository repository;
    @Mock
    @SuppressWarnings("unused")
    private WebClient ignoredWebClient;
    @Mock
    @SuppressWarnings("unused")
    private OAuthService oAuthService;
    @Mock
    private MetadataCollection metadataCollection;
    @InjectMocks
    private AdminClientFactory factory;

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
        when(metadataCollection.metadata(any()))
                .thenReturn(Mono.empty());

        // When
        var res = factory.getOrCreate(URI.create("http://localhost:8080").toURL());

        // Then
        StepVerifier.create(res)
                    .assertNext(resp -> assertThat(resp).isInstanceOf(CreatedAdminClientResponse.class))
                    .verifyComplete();
        verify(api, never()).carbonDataSpec(any());
    }

    @Test
    void testGetOrCreate_forCachedCdsServer_returnsClient() throws MalformedURLException {
        // Given
        var cdsServer = new CdsServerBuilder()
                .setBaseUri("http://localhost:8080")
                .build();
        when(repository.findByBaseUri("http://localhost:8080"))
                .thenReturn(Optional.of(cdsServer));

        when(metadataCollection.metadata(any()))
                .thenReturn(Mono.empty());

        // When
        var res = factory.getOrCreate(URI.create("http://localhost:8080").toURL())
                         .then(factory.getOrCreate(URI.create("http://localhost:8080/other/sub/path").toURL()));

        // Then
        StepVerifier.create(res)
                    .assertNext(resp -> assertThat(resp).isInstanceOf(CreatedAdminClientResponse.class))
                    .verifyComplete();
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

        // When
        var res = factory.getOrCreate(baseUri.toURL());

        // Then
        StepVerifier.create(res)
                    .assertNext(resp -> assertThat(resp).isInstanceOf(CreatedAdminClientResponse.class))
                    .verifyComplete();
        verify(repository).save(assertArg(cds -> assertAll(
                () -> assertEquals(baseUrl, cds.baseUri()),
                () -> assertEquals("CDS Server", cds.name()),
                () -> assertEquals(Set.of(EnergyType.ELECTRICITY, EnergyType.NATURAL_GAS), cds.coverages()),
                () -> assertEquals("client-id", cds.clientId()),
                () -> assertEquals("client-secret", cds.clientSecret())
        )));
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
        var res = factory.getOrCreate(baseUri.toURL());

        // Then
        StepVerifier.create(res)
                    .assertNext(resp -> assertThat(resp).isInstanceOf(CoverageNotSupportedResponse.class))
                    .verifyComplete();
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
        var res = factory.getOrCreate(baseUri.toURL());

        // Then
        StepVerifier.create(res)
                    .assertNext(resp -> assertThat(resp).isInstanceOf(NotACdsServerResponse.class))
                    .verifyComplete();
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
        var res = factory.getOrCreate(baseUri.toURL());

        // Then
        StepVerifier.create(res)
                    .assertNext(resp -> assertThat(resp).isInstanceOf(OAuthNotSupportedResponse.class))
                    .verifyComplete();
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
        var res = factory.getOrCreate(baseUri.toURL());

        // Then
        StepVerifier.create(res)
                    .assertNext(resp -> assertThat(resp).isInstanceOf(result))
                    .verifyComplete();
        verify(repository, never()).save(any());
    }


    @Test
    void testGet_forKnownCdsServer_returnsClient() {
        // Given
        var cdsServer = new CdsServerBuilder()
                .setBaseUri("http://localhost:8080")
                .build();
        when(repository.getReferenceById(1L))
                .thenReturn(cdsServer);

        // When
        var res = factory.get(1L);

        // Then
        StepVerifier.create(res)
                    .assertNext(resp -> assertThat(resp).isInstanceOf(CreatedAdminClientResponse.class))
                    .verifyComplete();
        verify(api, never()).carbonDataSpec(any());
    }

    @Test
    void testGet_forCachedCdsServer_returnsClient() {
        // Given
        var cdsServer = new CdsServerBuilder()
                .setBaseUri("http://localhost:8080")
                .build();
        when(repository.getReferenceById(1L))
                .thenReturn(cdsServer);

        // When
        var res = factory.get(1L)
                         .then(factory.get(1L));

        // Then
        StepVerifier.create(res)
                    .assertNext(resp -> assertThat(resp).isInstanceOf(CreatedAdminClientResponse.class))
                    .verifyComplete();
        verify(api, never()).carbonDataSpec(any());
    }
}