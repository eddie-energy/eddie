package energy.eddie.regionconnector.de.eta.streams;

import energy.eddie.regionconnector.de.eta.dtos.EtaMeteringDataPayload;
import energy.eddie.regionconnector.de.eta.dtos.IdentifiableValidatedHistoricalData;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;

/**
 * Stream component that publishes validated historical data from the ETA Plus API.
 * This stream is consumed by providers that convert the data to different formats
 * (raw data, CIM v0.82, CIM v1.04).
 */
@Component
public class ValidatedHistoricalDataStream implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatedHistoricalDataStream.class);

    private final Sinks.Many<IdentifiableValidatedHistoricalData> sink = Sinks.many()
            .multicast()
            .onBackpressureBuffer();

    /**
     * Get the validated historical data stream as a Flux.
     *
     * @return Flux of identifiable validated historical data
     */
    public Flux<IdentifiableValidatedHistoricalData> validatedHistoricalData() {
        return sink.asFlux();
    }

    /**
     * Publish metering data for a specific permission request.
     *
     * @param permissionRequest The permission request that triggered this data retrieval
     * @param data The metering data from the ETA Plus API
     */
    public void publish(DePermissionRequest permissionRequest, EtaMeteringDataPayload data) {
        LOGGER.atInfo()
                .addArgument(permissionRequest::permissionId)
                .addArgument(permissionRequest::meteringPointId)
                .log("Publishing validated historical data for permission request {} (metering point: {})");
        
        var identifiableData = new IdentifiableValidatedHistoricalData(permissionRequest, data);
        sink.emitNext(identifiableData, Sinks.EmitFailureHandler.busyLooping(Duration.ofMinutes(1)));
    }

    @Override
    public void close() {
        LOGGER.info("Closing validated historical data stream");
        sink.emitComplete(Sinks.EmitFailureHandler.busyLooping(Duration.ofMinutes(1)));
    }
}

