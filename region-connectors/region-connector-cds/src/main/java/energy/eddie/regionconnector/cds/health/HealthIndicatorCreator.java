package energy.eddie.regionconnector.cds.health;

import energy.eddie.regionconnector.cds.client.CdsPublicApis;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import org.springframework.boot.actuate.health.*;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class HealthIndicatorCreator {
    private static final String COMPOSITE_NAME = "region-connector-cds";
    private final HealthContributorRegistry registry;
    private final CdsPublicApis cdsPublicApis;
    private final CdsServerRepository repository;

    public HealthIndicatorCreator(
            HealthContributorRegistry registry,
            CdsPublicApis cdsPublicApis,
            CdsServerRepository repository
    ) {
        this.registry = registry;
        this.cdsPublicApis = cdsPublicApis;
        this.repository = repository;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationEvent() {
        registry.unregisterContributor(COMPOSITE_NAME);
        Map<String, HealthIndicator> indicators = new HashMap<>();
        for (var cdsServer : repository.findAll()) {
            var contributor = new CdsServerHealthIndicator(cdsPublicApis, cdsServer);
            indicators.put(contributorName(cdsServer), contributor);
        }
        var composite = CompositeHealthContributor.fromMap(indicators);
        registry.registerContributor(COMPOSITE_NAME, composite);
    }

    public void register(CdsServer cdsServer) {
        var contributor = new CdsServerHealthIndicator(cdsPublicApis, cdsServer);
        var oldComposite = registry.unregisterContributor(COMPOSITE_NAME);
        Map<String, HealthContributor> map;
        if (!(oldComposite instanceof CompositeHealthContributor chc)) {
            map = new HashMap<>();
        } else {
            map = chc.stream()
                     .collect(Collectors.toMap(NamedContributor::getName,
                                               NamedContributor::getContributor));
        }
        map.put(contributorName(cdsServer), contributor);
        registry.registerContributor(COMPOSITE_NAME, CompositeHealthContributor.fromMap(map));
    }

    private static String contributorName(CdsServer cdsServer) {
        return (cdsServer.name() + "-" + cdsServer.id()).replace(" ", "-");
    }
}
