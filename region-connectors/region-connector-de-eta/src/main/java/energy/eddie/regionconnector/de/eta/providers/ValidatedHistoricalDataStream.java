package energy.eddie.regionconnector.de.eta.providers;

import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

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

    public Flux<IdentifiableValidatedHistoricalData> validatedHistoricalData() {
        return flux;
    }

    /**
     * Publish a chunk of validated historical data.
     */
    public void publish(DePermissionRequest permissionRequest, EtaPlusMeteredData data) {
        var identifiableData = new IdentifiableValidatedHistoricalData(permissionRequest, data);

        Sinks.EmitResult result = sink.tryEmitNext(identifiableData);

        if (result.isFailure()) {
            LOGGER.error("Failed to emit data for permission {}: {}", permissionRequest.permissionId(), result);
        }
    }

    @Override
    public void close() {
        sink.tryEmitComplete();
    }
}