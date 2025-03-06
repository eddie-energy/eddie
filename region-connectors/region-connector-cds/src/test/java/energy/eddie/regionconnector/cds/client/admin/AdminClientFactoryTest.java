package energy.eddie.regionconnector.cds.client.admin;

import energy.eddie.regionconnector.cds.master.data.CdsServerBuilder;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import energy.eddie.regionconnector.cds.services.oauth.OAuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminClientFactoryTest {
    @Mock
    private CdsServerRepository repository;
    @Mock
    @SuppressWarnings("unused")
    private WebClient ignoredWebClient;
    @Mock
    @SuppressWarnings("unused")
    private OAuthService oAuthService;
    @InjectMocks
    private AdminClientFactory factory;


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
        assertThat(res).isPresent();
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
        factory.get(1L);
        var res = factory.get(1L);

        // Then
        assertThat(res).isPresent();
    }

    @Test
    void testGetTemporaryAdminClient_alwaysReturnsNewClient() {
        // Given
        var cdsServer = new CdsServerBuilder()
                .setBaseUri("http://localhost:8080")
                .build();

        // When
        var res1 = factory.getTemporaryAdminClient(cdsServer);
        var res2 = factory.getTemporaryAdminClient(cdsServer);

        // Then
        assertThat(res1).isNotSameAs(res2);
    }
}