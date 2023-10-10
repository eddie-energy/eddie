package energy.eddie.core;

import com.google.inject.Inject;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.RegionConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.util.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PermissionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionService.class);

    @Inject
    private Set<RegionConnector> regionConnectors;

    @Nullable
    public Flux<ConnectionStatusMessage> getConnectionStatusMessageStream() {

        List<Flux<ConnectionStatusMessage>> connectionStatusFluxes = new ArrayList<>(regionConnectors.size());
        for (var connector : regionConnectors) {
            try {
                connectionStatusFluxes.add(JdkFlowAdapter.flowPublisherToFlux(connector.getConnectionStatusMessageStream()));
            } catch (Exception e) {
                LOGGER.warn("Got no connection status message stream for connector {}", connector.getMetadata().mdaCode(), e);
            }
        }
        return Flux.merge(connectionStatusFluxes).share();
    }
}
