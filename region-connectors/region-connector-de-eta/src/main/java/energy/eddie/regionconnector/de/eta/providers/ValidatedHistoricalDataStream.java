package energy.eddie.regionconnector.de.eta.providers;

import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Central stream for validated historical data from ETA Plus.
 * This component manages the reactive stream of validated historical data,
 * allowing multiple subscribers (e.g., RawDataProvider, ValidatedHistoricalDataMarketDocumentProvider)
 * to receive the same data.
 */
@Component
public class ValidatedHistoricalDataStream implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatedHistoricalDataStream.class);

    private final Sinks.Many<IdentifiableValidatedHistoricalData> sink = Sinks.many()
            .multicast()
            .onBackpressureBuffer();
    private final Flux<IdentifiableValidatedHistoricalData> flux;

    public ValidatedHistoricalDataStream() {
        this.flux = sink.asFlux().share();
    }

    /**
     * Get the flux of validated historical data.
     * Multiple subscribers can subscribe to this flux.
     *
     * @return the flux of identifiable validated historical data
     */
    public Flux<IdentifiableValidatedHistoricalData> validatedHistoricalData() {
        return flux;
    }

    /**
     * Publish validated historical data to the stream.
     *
     * @param permissionRequest the permission request the data belongs to
     * @param data              the validated historical data from ETA Plus
     */
    public void publish(DePermissionRequest permissionRequest, EtaPlusMeteredData data) {
        LOGGER.atInfo()
              .addArgument(permissionRequest::permissionId)
              .addArgument(data::meteringPointId)
              .log("Publishing validated historical data for permission request {} with metering point {}");

        var identifiableData = new IdentifiableValidatedHistoricalData(permissionRequest, data);
        sink.tryEmitNext(identifiableData);
    }

    @Override
    public void close() {
        sink.tryEmitComplete();
    }
}

