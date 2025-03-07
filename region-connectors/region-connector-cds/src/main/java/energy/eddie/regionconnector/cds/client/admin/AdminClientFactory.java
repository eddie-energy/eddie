package energy.eddie.regionconnector.cds.client.admin;

import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import energy.eddie.regionconnector.cds.services.oauth.OAuthService;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AdminClientFactory {
    private final Map<Long, AdminClient> adminClients = new ConcurrentHashMap<>();
    private final CdsServerRepository cdsServerRepository;
    private final WebClient webClient;
    private final OAuthService oAuthService;

    public AdminClientFactory(
            CdsServerRepository cdsServerRepository,
            WebClient webClient,
            OAuthService oAuthService
    ) {
        this.cdsServerRepository = cdsServerRepository;
        this.webClient = webClient;
        this.oAuthService = oAuthService;
    }

    public Optional<AdminClient> get(Long id) {
        return lookUpAdminClient(id)
                .or(() -> createFromExistingCdsServer(id));
    }
    public AdminClient getTemporaryAdminClient(CdsServer cdsServer) {
        return new AdminClient(webClient, cdsServer, oAuthService);
    }

    private Optional<AdminClient> lookUpAdminClient(Long id) {
        return adminClients.containsKey(id)
                ? Optional.of(adminClients.get(id))
                : Optional.empty();
    }

    private Optional<AdminClient> createFromExistingCdsServer(Long id) {
        var cdsServer = cdsServerRepository.getReferenceById(id);
        var api = new AdminClient(webClient, cdsServer, oAuthService);
        adminClients.put(id, api);
        return Optional.of(api);
    }
}
