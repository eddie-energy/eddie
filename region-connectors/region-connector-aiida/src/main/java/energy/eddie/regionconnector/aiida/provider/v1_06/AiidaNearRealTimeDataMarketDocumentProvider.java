package energy.eddie.regionconnector.aiida.provider.v1_06;

import energy.eddie.api.v1_06.NearRealTimeDataMarketDocumentProviderV1_06;
import energy.eddie.cim.v1_06.rtd.RTDEnvelope;
import energy.eddie.regionconnector.aiida.streams.IdentifiableStreams;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component(value = "aiidaNearRealTimeDataMarketDocumentProviderV106")
public class AiidaNearRealTimeDataMarketDocumentProvider implements NearRealTimeDataMarketDocumentProviderV1_06 {
    private final Flux<RTDEnvelope> flux;

    public AiidaNearRealTimeDataMarketDocumentProvider(IdentifiableStreams streams) {
        this.flux = streams.nearRealTimeDataCimV106Flux();
    }

    @Override
    public Flux<RTDEnvelope> getNearRealTimeDataMarketDocumentsStream() {
        return flux;
    }
}
