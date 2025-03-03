package energy.eddie.regionconnector.cds.client.customer.data;

import energy.eddie.regionconnector.cds.client.Scopes;
import energy.eddie.regionconnector.cds.client.admin.AdminClient;
import energy.eddie.regionconnector.cds.client.admin.AdminClientFactory;
import energy.eddie.regionconnector.cds.client.admin.responses.CreatedAdminClientResponse;
import energy.eddie.regionconnector.cds.exceptions.NoCustomerDataClientFoundException;
import energy.eddie.regionconnector.cds.exceptions.UnknownPermissionAdministratorException;
import energy.eddie.regionconnector.cds.openapi.model.ClientEndpoint200ResponseClientsInner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CustomerDataClientFactory {
    private final Map<Long, CustomerDataClientCredentials> cachedCredentials = new HashMap<>();
    private final AdminClientFactory adminClientFactory;

    public CustomerDataClientFactory(AdminClientFactory adminClientFactory) {this.adminClientFactory = adminClientFactory;}

    public Mono<CustomerDataClientCredentials> create(Long id) {
        if (cachedCredentials.containsKey(id)) {
            return Mono.just(cachedCredentials.get(id));
        }
        return adminClientFactory.get(id)
                                 .flatMap(response -> switch (response) {
                                     case CreatedAdminClientResponse(AdminClient client) -> client.clients();
                                     default -> Mono.error(new UnknownPermissionAdministratorException(id));
                                 })
                                 .flatMap(this::createCustomerDataClientCredentials)
                                 .map(client -> {
                                     cachedCredentials.put(id, client);
                                     return client;
                                 });
    }

    private Mono<CustomerDataClientCredentials> createCustomerDataClientCredentials(List<ClientEndpoint200ResponseClientsInner> response) {
        for (var result : response) {
            if (!result.getScope().contains(Scopes.CUSTOMER_DATA_SCOPE)) {
                continue;
            }
            var clientId = result.getClientId();
            var clientCredentials = new CustomerDataClientCredentials(clientId);
            return Mono.just(clientCredentials);
        }
        return Mono.error(new NoCustomerDataClientFoundException());
    }
}
