package energy.eddie.core;

import com.google.inject.Inject;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.util.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EddieValidatedHistoricalDataMarketDocumentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EddieValidatedHistoricalDataMarketDocumentService.class);

    private final Set<RegionConnector> regionConnectors;

    @Inject
    public EddieValidatedHistoricalDataMarketDocumentService(Set<RegionConnector> regionConnectors) {
        this.regionConnectors = regionConnectors;
    }

    @Nullable
    public Flux<EddieValidatedHistoricalDataMarketDocument> getEddieValidatedHistoricalDataMarketDocumentStream() {
        List<Flux<EddieValidatedHistoricalDataMarketDocument>> consumptionRecordFluxes = new ArrayList<>(regionConnectors.size());
        for (var connector : regionConnectors) {
            if (connector instanceof energy.eddie.api.v0_82.RegionConnector rc) {
                try {
                    consumptionRecordFluxes.add(JdkFlowAdapter.flowPublisherToFlux(rc.getEddieValidatedHistoricalDataMarketDocumentStream()));
                } catch (Exception e) {
                    LOGGER.warn("Got no validated historical data market document stream for connector {}", connector.getMetadata().mdaCode(), e);
                }
            }
        }
        return Flux.merge(consumptionRecordFluxes).share();
    }
}