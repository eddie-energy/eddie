package energy.eddie.regionconnector.de.eta.providers;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.events.LatestMeterReadingEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.LocalDate;
import java.util.List;

/**
 * Central stream for validated historical data from ETA Plus.
 * This component manages the reactive stream of validated historical data AND
 * is responsible for tracking the latest meter reading for fulfillment checks.
 *
 * <p>Before emitting, data need constraints (energy type and granularity) are enforced:
 * if the received data does not satisfy the constraints stored on the permission request,
 * the request is marked {@link PermissionProcessStatus#UNFULFILLABLE} and no data is published.
 */
@Component
public class ValidatedHistoricalDataStream implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatedHistoricalDataStream.class);

    private final Sinks.Many<IdentifiableValidatedHistoricalData> sink = Sinks.many()
            .multicast()
            .onBackpressureBuffer();
    private final Flux<IdentifiableValidatedHistoricalData> flux;

    private final Outbox outbox;

    public ValidatedHistoricalDataStream(Outbox outbox) {
        this.outbox = outbox;
        this.flux = sink.asFlux().share();
    }

    /**
     * Get the flux of validated historical data.
     * Subscribers (like JsonRawDataProvider) use this.
     *
     * @return the flux of identifiable validated historical data
     */
    public Flux<IdentifiableValidatedHistoricalData> validatedHistoricalData() {
        return flux;
    }

    /**
     * Validates data need constraints and, if satisfied, publishes validated historical data
     * to the stream and updates the latest reading state.
     *
     * <p>When the permission request carries an energy type (VHD data needs), the following
     * checks are applied to the received readings:
     * <ul>
     *   <li><b>Energy type</b>: the unit of the first reading must correspond to
     *       {@link DePermissionRequest#energyType()}. A mismatch means the MDA is serving
     *       the wrong commodity (e.g. gas instead of electricity).</li>
     *   <li><b>Granularity</b>: when at least two readings are present, the interval between
     *       the first two sorted readings must match {@link DePermissionRequest#granularity()}.
     *       If the interval cannot be mapped to a known {@link Granularity}, it passes through
     *       (the MDA may have a temporary data gap).</li>
     * </ul>
     * If either check fails, a {@link PermissionProcessStatus#UNFULFILLABLE} event is committed
     * and no data is published to the stream.
     *
     * @param permissionRequest the permission request the data belongs to
     * @param data              the validated historical data from ETA Plus
     */
    public void publish(DePermissionRequest permissionRequest, EtaPlusMeteredData data) {
        LOGGER.atInfo()
                .addArgument(permissionRequest::permissionId)
                .addArgument(data::meteringPointId)
                .log("Publishing validated historical data for permission request {} with metering point {}");

        List<EtaPlusMeteredData.MeterReading> readings = data.readings();

        // Data need constraints apply only when energyType is set (VHD requests).
        // Accounting-point requests have null energyType and are not subject to these checks.
        if (readings != null && !readings.isEmpty() && permissionRequest.energyType() != null) {
            if (!isEnergyTypeValid(permissionRequest, readings)) {
                return;
            }
            if (!isGranularityValid(permissionRequest, readings)) {
                return;
            }
        }

        IdentifiableValidatedHistoricalData identifiableData = new IdentifiableValidatedHistoricalData(permissionRequest, data);
        emit(identifiableData);
        determineAndEmitLatestReading(identifiableData);
    }

    /**
     * Publishes retransmitted validated historical data to the outbound stream without mutating
     * permission state.
     *
     * <p>Unlike {@link #publish(DePermissionRequest, EtaPlusMeteredData)}, this method neither
     * updates the latest-meter-reading watermark nor emits any status event. A retransmission
     * re-delivers data the eligible party already received for a window in the past, so it must
     * not regress the fulfillment watermark — which would otherwise trigger redundant re-polling
     * of the whole range — nor change the permission's status (e.g. to
     * {@link PermissionProcessStatus#UNFULFILLABLE}). The data still reaches the outbound
     * connectors through the same stream consumed by the raw-data and CIM providers.
     *
     * @param permissionRequest the permission request the data belongs to
     * @param data              the validated historical data fetched for the retransmission window
     */
    public void publishRetransmission(DePermissionRequest permissionRequest, EtaPlusMeteredData data) {
        LOGGER.atInfo()
                .addArgument(permissionRequest::permissionId)
                .addArgument(data::meteringPointId)
                .log("Publishing retransmitted validated historical data for permission request {} with metering point {}");

        emit(new IdentifiableValidatedHistoricalData(permissionRequest, data));
    }

    private void emit(IdentifiableValidatedHistoricalData identifiableData) {
        Sinks.EmitResult emitResult = sink.tryEmitNext(identifiableData);
        if (emitResult.isFailure()) {
            LOGGER.warn("Failed to emit data to stream: {}", emitResult);
        }
    }

    private boolean isEnergyTypeValid(
            DePermissionRequest permissionRequest,
            List<EtaPlusMeteredData.MeterReading> readings
    ) {
        EnergyType actualEnergyType = EtaPlusVhdMappings.energyTypeFromUnit(readings.get(0).unit());
        if (actualEnergyType == permissionRequest.energyType()) {
            return true;
        }
        LOGGER.atWarn()
              .addArgument(permissionRequest::permissionId)
              .addArgument(permissionRequest::energyType)
              .addArgument(() -> readings.get(0).unit())
              .log("Discarding data for permission {}: expected energy type {} but MDA returned unit '{}'; marking UNFULFILLABLE");
        commitUnfulfillable(permissionRequest.permissionId());
        return false;
    }

    private boolean isGranularityValid(
            DePermissionRequest permissionRequest,
            List<EtaPlusMeteredData.MeterReading> readings
    ) {
        if (readings.size() < 2 || permissionRequest.granularity() == null) {
            return true;
        }
        List<EtaPlusMeteredData.MeterReading> sorted = EtaPlusVhdMappings.sortByTimestamp(readings);
        Granularity actualGranularity = EtaPlusVhdMappings.inferGranularity(sorted);
        if (actualGranularity == null || actualGranularity == permissionRequest.granularity()) {
            return true;
        }
        LOGGER.atWarn()
              .addArgument(permissionRequest::permissionId)
              .addArgument(permissionRequest::granularity)
              .addArgument(actualGranularity)
              .log("Discarding data for permission {}: required granularity {} but MDA returned {}; marking UNFULFILLABLE");
        commitUnfulfillable(permissionRequest.permissionId());
        return false;
    }

    private void commitUnfulfillable(String permissionId) {
        try {
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.UNFULFILLABLE));
        } catch (Exception ex) {
            LOGGER.atError()
                  .addArgument(permissionId)
                  .setCause(ex)
                  .log("Failed to persist UNFULFILLABLE event for permission {}");
        }
    }

    private void determineAndEmitLatestReading(IdentifiableValidatedHistoricalData identifiableData) {
        LocalDate latestDate = identifiableData.meterReadingEndDate();
        String permissionId = identifiableData.permissionRequest().permissionId();

        LOGGER.atDebug()
                .addArgument(permissionId)
                .addArgument(latestDate)
                .log("Identified latest meter reading end date for request {}: {}");

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