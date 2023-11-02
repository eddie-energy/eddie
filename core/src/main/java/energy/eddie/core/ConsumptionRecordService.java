package energy.eddie.core;

import com.google.inject.Inject;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.RegionConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.util.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ConsumptionRecordService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionRecordService.class);

    private final Set<RegionConnector> regionConnectors;

    @Inject
    public ConsumptionRecordService(Set<RegionConnector> regionConnectors) {
        this.regionConnectors = regionConnectors;
    }

    @Nullable
    public Flux<ConsumptionRecord> getConsumptionRecordStream() {
        List<Flux<ConsumptionRecord>> consumptionRecordFluxes = new ArrayList<>(regionConnectors.size());
        for (var connector : regionConnectors) {
            try {
                consumptionRecordFluxes.add(JdkFlowAdapter.flowPublisherToFlux(connector.getConsumptionRecordStream()));
            } catch (Exception e) {
                LOGGER.warn("Got no consumption record stream for connector {}", connector.getMetadata().mdaCode(), e);
            }
        }
        return Flux.merge(consumptionRecordFluxes).share();
    }
}
