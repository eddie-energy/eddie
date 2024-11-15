package energy.eddie.core.services;

import energy.eddie.api.agnostic.retransmission.RegionConnectorRetransmissionService;
import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.RetransmissionRequestRouter;
import energy.eddie.api.agnostic.retransmission.RetransmissionServiceNotFound;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This service routes retransmission messages between the region connectors. It does that by using the
 * regionConnectorId, which should match the region connector id.
 */
@Service
public class CoreRetransmissionRouter implements RetransmissionRequestRouter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreRetransmissionRouter.class);
    private final Map<String, RegionConnectorRetransmissionService> retransmissionServices = new HashMap<>();

    public void registerRetransmissionService(
            String regionConnectorId,
            RegionConnectorRetransmissionService regionConnectorRetransmissionService
    ) {
        LOGGER.info("RetransmissionRouter: Registering {}", regionConnectorRetransmissionService.getClass().getName());
        retransmissionServices.put(regionConnectorId, regionConnectorRetransmissionService);
    }

    @Override
    public Mono<RetransmissionResult> routeRetransmissionRequest(
            String regionConnectorId,
            RetransmissionRequest retransmissionRequest
    ) {
        LOGGER.info("Routing RetransmissionRequest for region connector ID {}, request: {}",
                    regionConnectorId,
                    retransmissionRequest);

        return Optional.ofNullable(retransmissionServices.get(regionConnectorId))
                       .map(service -> service.requestRetransmission(retransmissionRequest))
                       .orElse(Mono.error(new RetransmissionServiceNotFound(
                               "No retransmission service registered for region connector with id " + regionConnectorId
                       )));
    }
}
