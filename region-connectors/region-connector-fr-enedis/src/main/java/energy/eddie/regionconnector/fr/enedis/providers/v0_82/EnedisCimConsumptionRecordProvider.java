package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;
import energy.eddie.regionconnector.fr.enedis.providers.agnostic.IdentifiableMeterReading;
import org.springframework.stereotype.Component;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;

import java.util.concurrent.Flow;

@Component
public class EnedisCimConsumptionRecordProvider implements energy.eddie.api.v0_82.CimConsumptionRecordProvider {

    private final Flux<IdentifiableMeterReading> identifiableMeterReadings;
    private final IntermediateVHDFactory intermediateVHDFactory;

    public EnedisCimConsumptionRecordProvider(Flux<IdentifiableMeterReading> identifiableMeterReadings,
                                              IntermediateVHDFactory intermediateVHDFactory) {
        this.identifiableMeterReadings = identifiableMeterReadings;
        this.intermediateVHDFactory = intermediateVHDFactory;
    }

    @Override
    public Flow.Publisher<EddieValidatedHistoricalDataMarketDocument> getEddieValidatedHistoricalDataMarketDocumentStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(
                identifiableMeterReadings
                        .map(intermediateVHDFactory::create)
                        .map(IntermediateValidatedHistoricalDocument::eddieValidatedHistoricalDataMarketDocument)
        );
    }

    @Override
    public void close() throws Exception {
        // No-Op
    }
}
