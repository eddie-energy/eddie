package energy.eddie.core.services.v1_06;

import energy.eddie.api.v1_06.NearRealTimeDataMarketDocumentProviderV1_06;
import energy.eddie.cim.v1_06.rtd.RTDEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service(value = "nearRealTimeDataMarketDocumentServiceV106")
public class NearRealTimeDataMarketDocumentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NearRealTimeDataMarketDocumentService.class);

    private final Sinks.Many<RTDEnvelope> rtdSink = Sinks.many()
                                                         .multicast()
                                                         .onBackpressureBuffer();

    public void registerProvider(NearRealTimeDataMarketDocumentProviderV1_06 provider) {
        LOGGER.info("Registering {}", provider.getClass().getName());
        provider.getNearRealTimeDataMarketDocumentsStream()
                .onErrorContinue((err, obj) -> LOGGER.warn(
                        "Encountered error while processing near real-time data market document",
                        err))
                .subscribe(rtdSink::tryEmitNext);
    }

    public Flux<RTDEnvelope> getNearRealTimeDataMarketDocumentStream() {
        return rtdSink.asFlux();
    }
}