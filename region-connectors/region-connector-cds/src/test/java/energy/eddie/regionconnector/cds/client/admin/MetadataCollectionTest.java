package energy.eddie.regionconnector.cds.client.admin;

import energy.eddie.regionconnector.cds.client.CdsPublicApis;
import energy.eddie.regionconnector.cds.exceptions.CoverageNotSupportedException;
import energy.eddie.regionconnector.cds.exceptions.OAuthNotSupportedException;
import energy.eddie.regionconnector.cds.openapi.model.CarbonDataSpec200Response;
import energy.eddie.regionconnector.cds.openapi.model.Coverages200ResponseAllOfCoverageEntriesInner;
import energy.eddie.regionconnector.cds.openapi.model.OAuthAuthorizationServer200Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetadataCollectionTest {
    @Mock
    private CdsPublicApis apis;
    @InjectMocks
    private MetadataCollection collection;

    @Test
    void testMetadata_returnsAllMetadata() {
        // Given
        var baseUri = URI.create("http://localhost");
        when(apis.carbonDataSpec(baseUri))
                .thenReturn(Mono.just(new CarbonDataSpec200Response()
                                              .oauthMetadata(baseUri)
                                              .coverage(baseUri)
                                              .capabilities(List.of("coverage"))));
        when(apis.oauthMetadataSpec(baseUri))
                .thenReturn(Mono.just(new OAuthAuthorizationServer200Response()));
        when(apis.coverage(baseUri))
                .thenReturn(Mono.just(List.of(new Coverages200ResponseAllOfCoverageEntriesInner())));

        // When
        var res = collection.metadata(baseUri);

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void testMetadata_withMissingCoverageCapability_throwsCoverageNotSupported() {
        // Given
        var baseUri = URI.create("http://localhost");
        when(apis.carbonDataSpec(baseUri))
                .thenReturn(Mono.just(new CarbonDataSpec200Response()
                                              .oauthMetadata(baseUri)
                                              .coverage(baseUri)
                                              .capabilities(List.of())));

        // When
        var res = collection.metadata(baseUri);

        // Then
        StepVerifier.create(res)
                    .expectError(CoverageNotSupportedException.class)
                    .verify();
    }


    @Test
    void testMetadata_withMissingOAuthEndpoint_throwsOAuthNotSupportedException() {
        // Given
        var baseUri = URI.create("http://localhost");
        when(apis.carbonDataSpec(baseUri))
                .thenReturn(Mono.just(new CarbonDataSpec200Response()
                                              .coverage(baseUri)
                                              .capabilities(List.of("coverage"))));

        // When
        var res = collection.metadata(baseUri);

        // Then
        StepVerifier.create(res)
                    .expectError(OAuthNotSupportedException.class)
                    .verify();
    }
}