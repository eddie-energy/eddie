package energy.eddie.regionconnector.cds.client.customer.data;

import energy.eddie.regionconnector.cds.client.Scopes;
import energy.eddie.regionconnector.cds.client.admin.AdminClient;
import energy.eddie.regionconnector.cds.client.admin.AdminClientFactory;
import energy.eddie.regionconnector.cds.client.admin.responses.CreatedAdminClientResponse;
import energy.eddie.regionconnector.cds.client.admin.responses.OAuthNotSupportedResponse;
import energy.eddie.regionconnector.cds.exceptions.NoCustomerDataClientFoundException;
import energy.eddie.regionconnector.cds.exceptions.UnknownPermissionAdministratorException;
import energy.eddie.regionconnector.cds.openapi.model.ClientEndpoint200ResponseClientsInner;
import energy.eddie.regionconnector.cds.services.oauth.OAuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class CustomerDataClientFactoryTest {
    @Mock
    private AdminClientFactory adminClientFactory;
    @Mock
    @SuppressWarnings("unused")
    private OAuthService oAuthService;
    @Mock
    private AdminClient adminClient;
    @InjectMocks
    private CustomerDataClientFactory customerDataClientFactory;

    @Test
    void testCreate_withoutCache_returnsClient() {
        // Given
        when(adminClientFactory.get(1L))
                .thenReturn(Mono.just(new CreatedAdminClientResponse(adminClient)));
        var oAuthClientCreds = List.of(
                new ClientEndpoint200ResponseClientsInner()
                        .scope(Scopes.CLIENT_ADMIN_SCOPE),
                new ClientEndpoint200ResponseClientsInner()
                        .clientId("customer-client")
                        .scope(Scopes.CUSTOMER_DATA_SCOPE)
        );
        when(adminClient.clients())
                .thenReturn(Mono.just(oAuthClientCreds));

        // When
        var res = customerDataClientFactory.create(1L);

        // Then
        StepVerifier.create(res)
                    .assertNext(client -> assertEquals("customer-client", client.clientId()))
                    .verifyComplete();
    }

    @Test
    void testCreate_withCache_returnsClient() {
        // Given
        when(adminClientFactory.get(1L))
                .thenReturn(Mono.just(new CreatedAdminClientResponse(adminClient)));
        var oAuthClientCreds = List.of(
                new ClientEndpoint200ResponseClientsInner()
                        .scope(Scopes.CLIENT_ADMIN_SCOPE),
                new ClientEndpoint200ResponseClientsInner()
                        .clientId("customer-client")
                        .scope(Scopes.CUSTOMER_DATA_SCOPE)
        );
        when(adminClient.clients())
                .thenReturn(Mono.just(oAuthClientCreds));

        // When
        var res = customerDataClientFactory.create(1L)
                                           .then(Mono.defer(() -> customerDataClientFactory.create(1L)));

        // Then
        StepVerifier.create(res)
                    .assertNext(client -> assertEquals("customer-client", client.clientId()))
                    .verifyComplete();
    }


    @Test
    void testCreate_withInvalidAdminClient_throwsUnknownPermissionAdministratorException() {
        // Given
        when(adminClientFactory.get(1L))
                .thenReturn(Mono.just(new OAuthNotSupportedResponse()));

        // When
        var res = customerDataClientFactory.create(1L);

        // Then
        StepVerifier.create(res)
                    .expectError(UnknownPermissionAdministratorException.class)
                    .verify();
    }

    @Test
    void testCreate_withInvalidClientResponse_throwsNoCustomerDataClientFoundException() {
        // Given
        when(adminClientFactory.get(1L))
                .thenReturn(Mono.just(new CreatedAdminClientResponse(adminClient)));
        var oAuthClientCreds = List.of(
                new ClientEndpoint200ResponseClientsInner()
                        .scope(Scopes.CLIENT_ADMIN_SCOPE)
        );
        when(adminClient.clients())
                .thenReturn(Mono.just(oAuthClientCreds));

        // When
        var res = customerDataClientFactory.create(1L);

        // Then
        StepVerifier.create(res)
                    .expectError(NoCustomerDataClientFoundException.class)
                    .verify();
    }
}