package energy.eddie.core.services;

import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.RawDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Sinks;

import java.util.concurrent.Flow;

@Service
@ConditionalOnProperty(name = "eddie.raw.data.output.enabled", havingValue = "true")
public class RawDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RawDataService.class);

    private final Sinks.Many<RawDataMessage> rawDataSink = Sinks.many().multicast().onBackpressureBuffer();

    public void registerProvider(RawDataProvider rawDataProvider) {
        LOGGER.info("RawDataService: Registering {}", rawDataProvider.getClass().getName());
        JdkFlowAdapter.flowPublisherToFlux(rawDataProvider.getRawDataStream())
                .doOnNext(rawDataSink::tryEmitNext)
                .doOnError(rawDataSink::tryEmitError)
                .subscribe();
    }

    public Flow.Publisher<RawDataMessage> getRawDataStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(rawDataSink.asFlux());
    }
}