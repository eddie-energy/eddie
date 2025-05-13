package energy.eddie.regionconnector.cds.client;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import energy.eddie.regionconnector.cds.client.admin.AdminClient;
import energy.eddie.regionconnector.cds.client.customer.data.CustomerDataClient;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import energy.eddie.regionconnector.cds.services.oauth.CustomerDataTokenService;
import energy.eddie.regionconnector.cds.services.oauth.OAuthService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Optional;

@Component
public class CdsServerClientFactory {
    private final CdsPublicApis cdsPublicApis;
    private final AdminClient adminClient;
    private final CustomerDataClient customerDataClient;
    private final CdsServerRepository repository;
    private final OAuthService oAuthService;
    private final CustomerDataTokenService customerDataTokenService;
    private final LoadingCache<Long, CdsServerClient> clientCache =
            Caffeine.newBuilder()
                    .maximumSize(100)
                    .build(this::create);

    public CdsServerClientFactory(
            CdsPublicApis cdsPublicApis,
            AdminClient adminClient,
            CustomerDataClient customerDataClient,
            CdsServerRepository repository,
            OAuthService oAuthService, CustomerDataTokenService customerDataTokenService
    ) {
        this.cdsPublicApis = cdsPublicApis;
        this.adminClient = adminClient;
        this.customerDataClient = customerDataClient;
        this.repository = repository;
        this.oAuthService = oAuthService;
        this.customerDataTokenService = customerDataTokenService;
    }

    public Flux<CdsServerClient> getAll() {
        var servers = repository.findAll();
        return Flux.create(sink -> {
            for (var server : servers) {
                sink.next(get(server));
            }
            sink.complete();
        });
    }

    public Optional<CdsServerClient> get(Long id) {
        var client = clientCache.getIfPresent(id);
        if (client != null) {
            return Optional.of(client);
        }
        var cdsServerClient = repository.findById(id)
                                        .map(this::create);
        cdsServerClient.ifPresent(resp -> clientCache.put(id, resp));
        return cdsServerClient;
    }

    public CdsServerClient get(CdsServer cdsServer) {
        return clientCache.get(cdsServer.id());
    }

    public CdsServerClient getTemporary(CdsServer cdsServer) {
        return create(cdsServer);
    }

    public CdsServerClient get(CdsPermissionRequest permissionRequest) {
        var cdsServerId = permissionRequest.dataSourceInformation().cdsServerId();
        return clientCache.get(cdsServerId);
    }

    private CdsServerClient create(Long id) {
        var cdsServer = repository.getReferenceById(id);
        return create(cdsServer);
    }

    private CdsServerClient create(CdsServer cdsServer) {
        return new CdsServerClient(cdsServer,
                                   cdsPublicApis,
                                   adminClient,
                                   customerDataClient,
                                   oAuthService,
                                   customerDataTokenService);
    }
}
