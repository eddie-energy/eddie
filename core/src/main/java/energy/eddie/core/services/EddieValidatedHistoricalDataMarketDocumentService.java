package energy.eddie.core.services;

import energy.eddie.api.v0_82.EddieValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Sinks;

import java.util.concurrent.Flow;

@Service
public class EddieValidatedHistoricalDataMarketDocumentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(energy.eddie.core.services.EddieValidatedHistoricalDataMarketDocumentService.class);

    private final Sinks.Many<EddieValidatedHistoricalDataMarketDocument> consumptionRecordSink = Sinks.many().multicast().onBackpressureBuffer();

    public void registerProvider(EddieValidatedHistoricalDataMarketDocumentProvider eddieValidatedHistoricalDataMarketDocumentProvider) {
        LOGGER.info("EddieValidatedHistoricalDataMarketDocumentService: Registering {}", eddieValidatedHistoricalDataMarketDocumentProvider.getClass().getName());
        JdkFlowAdapter.flowPublisherToFlux(eddieValidatedHistoricalDataMarketDocumentProvider.getEddieValidatedHistoricalDataMarketDocumentStream())
                .doOnNext(consumptionRecordSink::tryEmitNext)
                .doOnError(consumptionRecordSink::tryEmitError)
                .subscribe();
    }

    public Flow.Publisher<EddieValidatedHistoricalDataMarketDocument> getEddieValidatedHistoricalDataMarketDocumentStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(consumptionRecordSink.asFlux());
    }
}