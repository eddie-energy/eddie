package energy.eddie.framework;

import com.google.inject.Inject;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.RegionConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.util.annotation.Nullable;

import java.util.Set;

public class ConsumptionRecordService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionRecordService.class);

    @Inject
    private Set<RegionConnector> regionConnectors;

    @Nullable
    public Flux<ConsumptionRecord> getConsumptionRecordStream() {
        Flux<ConsumptionRecord> result = null;
        for (var connector : regionConnectors) {
            try {
                result = JdkFlowAdapter.flowPublisherToFlux(connector.getConsumptionRecordStream());
            } catch (Exception e) {
                LOGGER.warn("got no consumption record stream for connector {}", connector.getMetadata().mdaCode(), e);
            }
        }
        return result;
    }
}
