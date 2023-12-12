package energy.eddie.regionconnector.aiida;

import energy.eddie.api.v0.HealthState;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AiidaRegionConnector implements RegionConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiidaRegionConnector.class);
    private final AiidaRegionConnectorService aiidaService;

    public AiidaRegionConnector(AiidaRegionConnectorService aiidaService) {
        this.aiidaService = aiidaService;
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return AiidaRegionConnectorMetadata.getInstance();
    }

    @Override
    public void terminatePermission(String permissionId) {
        try {
            aiidaService.terminatePermission(permissionId);
        } catch (StateTransitionException e) {
            LOGGER.error("Error while terminating permission {}", permissionId, e);
        }
    }

    @Override
    public Map<String, HealthState> health() {
        return Map.of(getMetadata().id(), HealthState.UP);
    }
}
