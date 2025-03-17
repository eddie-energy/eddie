package energy.eddie.regionconnector.cds.client.customer.data;

import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import energy.eddie.regionconnector.cds.services.oauth.CustomerDataTokenService;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Component
public class CustomerDataClientFactory {
    private final Map<Long, CustomerDataClient> clients = new HashMap<>();
    private final CdsServerRepository repository;
    private final WebClient webClient;
    private final CustomerDataTokenService tokenService;

    public CustomerDataClientFactory(
            CdsServerRepository repository, WebClient webClient,
            CustomerDataTokenService tokenService
    ) {
        this.repository = repository;
        this.webClient = webClient;
        this.tokenService = tokenService;
    }

    public CustomerDataClient get(CdsPermissionRequest permissionRequest) {
        var cdsServerId = permissionRequest.dataSourceInformation().cdsServerId();
        if (clients.containsKey(cdsServerId)) {
            return clients.get(cdsServerId);
        }
        var cdsServer = repository.getReferenceById(cdsServerId);
        var client = new CustomerDataClient(webClient, cdsServer, tokenService);
        clients.put(cdsServerId, client);
        return client;
    }
}
