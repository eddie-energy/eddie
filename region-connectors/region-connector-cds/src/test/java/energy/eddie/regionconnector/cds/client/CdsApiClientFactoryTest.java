package energy.eddie.regionconnector.cds.client;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.regionconnector.cds.client.responses.*;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.openapi.model.CarbonDataSpec200Response;
import energy.eddie.regionconnector.cds.openapi.model.Coverages200ResponseAllOfCoverageEntriesInner;
import energy.eddie.regionconnector.cds.openapi.model.OAuthAuthorizationServer200Response;
import energy.eddie.regionconnector.cds.openapi.model.OAuthClientRegistration200Response;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
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
import reactor.test.StepVerifier;

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
class CdsApiClientFactoryTest {
    @Mock
    private CdsPublicApis api;
    @Mock
    private CdsServerRepository repository;
    @InjectMocks
    private CdsApiClientFactory factory;

    public static Stream<Arguments> testGetCdsApiClient_forUnknownCdsServer_returnsGrantTypeNotSupported_whenGrantTypeNotSupported() {
        return Stream.of(
                Arguments.of("authorization_code", RefreshTokenGrantTypeNotSupported.class),
                Arguments.of("refresh_token", AuthorizationCodeGrantTypeNotSupported.class)
        );
    }

    @Test
    void testGetCdsApiClient_forKnownCdsServer_returnsClient() throws MalformedURLException {
        // Given
        when(repository.findByBaseUri("http://localhost:8080"))
                .thenReturn(Optional.of(new CdsServer("http://localhost:8080",
                                                      "CDS Server",
                                                      Set.of(),
                                                      "client-id",
                                                      "client-secret")));

        // When
        var res = factory.getCdsApiClient(URI.create("http://localhost:8080").toURL());

        // Then
        StepVerifier.create(res)
                    .assertNext(resp -> assertThat(resp).isInstanceOf(CreatedApiClientResponse.class))
                    .verifyComplete();
        verify(api, never()).carbonDataSpec(any());
    }

    @Test
    void testGetCdsApiClient_forCachedCdsServer_returnsClient() throws MalformedURLException {
        // Given
        when(repository.findByBaseUri("http://localhost:8080"))
                .thenReturn(Optional.of(new CdsServer("http://localhost:8080",
                                                      "CDS Server",
                                                      Set.of(),
                                                      "client-id",
                                                      "client-secret")));

        // When
        var res = factory.getCdsApiClient(URI.create("http://localhost:8080").toURL())
                         .then(factory.getCdsApiClient(URI.create("http://localhost:8080/other/sub/path").toURL()));

        // Then
        StepVerifier.create(res)
                    .assertNext(resp -> assertThat(resp).isInstanceOf(CreatedApiClientResponse.class))
                    .verifyComplete();
        verify(api, never()).carbonDataSpec(any());
        verify(repository).findByBaseUri(any());
    }

    @Test
    void testGetCdsApiClient_forUnknownCdsServer_createsAndReturnsClient() throws MalformedURLException {
        // Given
        var baseUrl = "http://localhost:8080";
        var baseUri = URI.create(baseUrl);
        when(repository.findByBaseUri(baseUrl)).thenReturn(Optional.empty());
        when(api.carbonDataSpec(baseUri))
                .thenReturn(Mono.just(
                        new CarbonDataSpec200Response()
                                .name("CDS Server")
                                .coverage(baseUri)
                                .oauthMetadata(baseUri)
                                .capabilities(List.of("coverage", "oauth"))

                ));
        when(api.oauthMetadataSpec(baseUri))
                .thenReturn(Mono.just(
                        new OAuthAuthorizationServer200Response()
                                .grantTypesSupported(List.of("authorization_code", "refresh_token"))
                                .registrationEndpoint(baseUri)
                ));
        when(api.coverage(baseUri))
                .thenReturn(Mono.just(
                        List.of(
                                new Coverages200ResponseAllOfCoverageEntriesInner()
                                        .commodityTypes(Arrays.asList(Coverages200ResponseAllOfCoverageEntriesInner.CommodityTypesEnum.values()))
                        )
                ));
        when(api.createOAuthClient(baseUri))
                .thenReturn(Mono.just(
                        new OAuthClientRegistration200Response()
                                .clientId("client-id")
                                .clientSecret("client-secret")
                                .cdsServerMetadata(baseUri)
                ));

        // When
        var res = factory.getCdsApiClient(baseUri.toURL());

        // Then
        StepVerifier.create(res)
                    .assertNext(resp -> assertThat(resp).isInstanceOf(CreatedApiClientResponse.class))
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
    void testGetCdsApiClient_forUnknownCdsServer_returnsCoverageNotSupported_forCDSServerThatDoesNotSupportCoverage() throws MalformedURLException {
        // Given
        var baseUrl = "http://localhost:8080";
        var baseUri = URI.create(baseUrl);
        when(repository.findByBaseUri(baseUrl)).thenReturn(Optional.empty());
        when(api.carbonDataSpec(baseUri))
                .thenReturn(Mono.just(
                        new CarbonDataSpec200Response()
                                .name("CDS Server")
                                .oauthMetadata(baseUri)
                                .capabilities(List.of("oauth"))

                ));

        // When
        var res = factory.getCdsApiClient(baseUri.toURL());

        // Then
        StepVerifier.create(res)
                    .assertNext(resp -> assertThat(resp).isInstanceOf(CoverageNotSupportedResponse.class))
                    .verifyComplete();
        verify(repository, never()).save(any());
    }

    @Test
    void testGetCdsApiClient_forUnknownCdsServer_returnsNotACdsServer_forInvalidCdsServer() throws MalformedURLException {
        // Given
        var baseUrl = "http://localhost:8080";
        var baseUri = URI.create(baseUrl);
        when(repository.findByBaseUri(baseUrl)).thenReturn(Optional.empty());
        when(api.carbonDataSpec(baseUri))
                .thenReturn(Mono.error(WebClientResponseException.create(404, "text", null, null, null)));

        // When
        var res = factory.getCdsApiClient(baseUri.toURL());

        // Then
        StepVerifier.create(res)
                    .assertNext(resp -> assertThat(resp).isInstanceOf(NotACdsServerResponse.class))
                    .verifyComplete();
        verify(repository, never()).save(any());
    }

    @Test
    void testGetCdsApiClient_forUnknownCdsServer_returnsOAuthNotSupported_forCDSServerThatDoesNotSupportOAuth() throws MalformedURLException {
        // Given
        var baseUrl = "http://localhost:8080";
        var baseUri = URI.create(baseUrl);
        when(repository.findByBaseUri(baseUrl)).thenReturn(Optional.empty());
        when(api.carbonDataSpec(baseUri))
                .thenReturn(Mono.just(
                        new CarbonDataSpec200Response()
                                .name("CDS Server")
                                .coverage(baseUri)
                                .capabilities(List.of("coverage"))

                ));

        // When
        var res = factory.getCdsApiClient(baseUri.toURL());

        // Then
        StepVerifier.create(res)
                    .assertNext(resp -> assertThat(resp).isInstanceOf(OAuthNotSupportedResponse.class))
                    .verifyComplete();
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @MethodSource
    void testGetCdsApiClient_forUnknownCdsServer_returnsGrantTypeNotSupported_whenGrantTypeNotSupported(
            String grantType,
            Class<?> result
    ) throws MalformedURLException {
        // Given
        var baseUrl = "http://localhost:8080";
        var baseUri = URI.create(baseUrl);
        when(repository.findByBaseUri(baseUrl)).thenReturn(Optional.empty());
        when(api.carbonDataSpec(baseUri))
                .thenReturn(Mono.just(
                        new CarbonDataSpec200Response()
                                .name("CDS Server")
                                .coverage(baseUri)
                                .oauthMetadata(baseUri)
                                .capabilities(List.of("coverage", "oauth"))

                ));
        when(api.oauthMetadataSpec(baseUri))
                .thenReturn(Mono.just(
                        new OAuthAuthorizationServer200Response()
                                .grantTypesSupported(List.of(grantType))
                                .registrationEndpoint(baseUri)
                ));
        when(api.coverage(baseUri))
                .thenReturn(Mono.just(
                        List.of(
                                new Coverages200ResponseAllOfCoverageEntriesInner()
                                        .commodityTypes(Arrays.asList(Coverages200ResponseAllOfCoverageEntriesInner.CommodityTypesEnum.values()))
                        )
                ));

        // When
        var res = factory.getCdsApiClient(baseUri.toURL());

        // Then
        StepVerifier.create(res)
                    .assertNext(resp -> assertThat(resp).isInstanceOf(result))
                    .verifyComplete();
        verify(repository, never()).save(any());
    }
}