package energy.eddie.regionconnector.cds.client.customer.data;

import energy.eddie.regionconnector.cds.master.data.CdsServerBuilder;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import energy.eddie.regionconnector.cds.services.oauth.CustomerDataTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerDataClientFactoryTest {
    @Spy
    private final WebClient webClient = WebClient.create();
    @Mock
    private CdsServerRepository repository;
    @Mock
    private CustomerDataTokenService tokenService;
    @InjectMocks
    private CustomerDataClientFactory factory;

    @Test
    void testGet_returnsClient() {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setCdsServer(1L)
                .build();
        when(repository.getReferenceById(1L))
                .thenReturn(new CdsServerBuilder().build());

        // When
        var res = factory.get(pr);

        // Then
        assertNotNull(res);
    }

    @Test
    void testGet_requestingSameClientTwice_returnsCachedClient() {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setCdsServer(1L)
                .build();
        when(repository.getReferenceById(1L))
                .thenReturn(new CdsServerBuilder().build());

        // When
        var res1 = factory.get(pr);
        var res2 = factory.get(pr);

        // Then
        assertSame(res1, res2);
    }
}