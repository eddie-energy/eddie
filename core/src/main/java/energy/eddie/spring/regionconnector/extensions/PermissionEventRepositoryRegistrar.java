package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.core.services.PermissionEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Supplier;

@RegionConnectorExtension
public class PermissionEventRepositoryRegistrar {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionEventRepositoryRegistrar.class);

    public PermissionEventRepositoryRegistrar(Optional<Supplier<PermissionEventRepository>> supplier,
                                              RegionConnector regionconnector, PermissionEventService service) {
        String regionConnectorId = regionconnector.getMetadata().id();
        if (supplier.isPresent()) {
            service.registerPermissionEventRepository(supplier.get().get(), regionConnectorId);
            LOGGER.info("PermissionEventService: Registering PermissionEventRepository {}", regionConnectorId);
        } else {
            LOGGER.info("PermissionEventService: No PermissionEventRepository was registered {}", regionConnectorId);
        }
    }
}
