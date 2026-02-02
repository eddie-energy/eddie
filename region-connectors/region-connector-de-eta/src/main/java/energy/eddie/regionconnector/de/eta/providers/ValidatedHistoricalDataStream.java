package energy.eddie.regionconnector.de.eta.providers;

import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.events.LatestMeterReadingEvent;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.LocalDate;

/**
 * Central stream for validated historical data from ETA Plus.
 * This component manages the reactive stream of validated historical data AND
 * is responsible for tracking the latest meter reading for fulfillment checks.
 */
@Component
public class ValidatedHistoricalDataStream implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatedHistoricalDataStream.class);

    private final Sinks.Many<IdentifiableValidatedHistoricalData> sink = Sinks.many()
            .multicast()
            .onBackpressureBuffer();
    private final Flux<IdentifiableValidatedHistoricalData> flux;

    // We need the Outbox to emit the LatestMeterReadingEvent
    private final Outbox outbox;

    public ValidatedHistoricalDataStream(Outbox outbox) {
        this.outbox = outbox;
        this.flux = sink.asFlux().share();
    }

    /**
     * Get the flux of validated historical data.
     * Subscribers (like DeRawDataProvider) use this.
     *
     * @return the flux of identifiable validated historical data
     */
    public Flux<IdentifiableValidatedHistoricalData> validatedHistoricalData() {
        return flux;
    }

    /**
     * Publish validated historical data to the stream and update latest reading state.
     *
     * @param permissionRequest the permission request the data belongs to
     * @param data              the validated historical data from ETA Plus
     */
    public void publish(DePermissionRequest permissionRequest, EtaPlusMeteredData data) {
        LOGGER.atInfo()
                .addArgument(permissionRequest::permissionId)
                .addArgument(data::meteringPointId)
                .log("Publishing validated historical data for permission request {} with metering point {}");

        // 1. Emit to the reactive stream (for Raw Data & Market Documents)
        // Using explicit type as requested
        IdentifiableValidatedHistoricalData identifiableData = new IdentifiableValidatedHistoricalData(permissionRequest, data);
        Sinks.EmitResult emitResult = sink.tryEmitNext(identifiableData);

        if (emitResult.isFailure()) {
            LOGGER.warn("Failed to emit data to stream: {}", emitResult);
        }

        // 2. Calculate and emit LatestMeterReadingEvent (Task 5)
        // Using the interface method from your colleague's class
        determineAndEmitLatestReading(identifiableData);
    }

    private void determineAndEmitLatestReading(IdentifiableValidatedHistoricalData identifiableData) {
        LocalDate latestDate = identifiableData.meterReadingEndDate();
        String permissionId = identifiableData.permissionRequest().permissionId();

        LOGGER.atDebug()
                .addArgument(permissionId)
                .addArgument(latestDate)
                .log("Identified latest meter reading end date for request {}: {}");

        // Emit event to update the view and trigger fulfillment check
        outbox.commit(new LatestMeterReadingEvent(
                permissionId,
                latestDate
        ));
    }

    @Override
    public void close() {
        sink.tryEmitComplete();
    }
}