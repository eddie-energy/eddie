package energy.eddie.regionconnector.fi.fingrid.services;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.regionconnector.fi.fingrid.client.model.CustomerDataResponse;
import energy.eddie.regionconnector.fi.fingrid.client.model.TimeSeriesResponse;
import energy.eddie.regionconnector.fi.fingrid.permission.events.MeterReadingEvent;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequest;
import energy.eddie.regionconnector.fi.fingrid.services.cim.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.fi.fingrid.services.cim.IdentifiableValidatedHistoricalData;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.ObjectMapper;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

@Service
public class EnergyDataService implements RawDataProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnergyDataService.class);
    private final Sinks.Many<RawDataMessage> rawData = Sinks.many().multicast().onBackpressureBuffer();
    private final ObjectMapper objectMapper;
    private final Outbox outbox;
    private final Sinks.Many<IdentifiableAccountingPointData> aps = Sinks.many()
                                                                         .multicast()
                                                                         .onBackpressureBuffer();

    private final Sinks.Many<IdentifiableValidatedHistoricalData> vhds = Sinks.many()
                                                                              .multicast()
                                                                              .onBackpressureBuffer();

    public EnergyDataService(ObjectMapper objectMapper, Outbox outbox) {
        this.objectMapper = objectMapper;
        this.outbox = outbox;
    }

    public Consumer<List<TimeSeriesResponse>> publish(FingridPermissionRequest permissionRequest) {
        return response -> publish(response, permissionRequest);
    }

    public void publish(List<TimeSeriesResponse> timeSeriesResponses, FingridPermissionRequest permissionRequest) {
        publishWithoutUpdating(timeSeriesResponses, permissionRequest);
        publishLatestMeterReading(permissionRequest, timeSeriesResponses);
    }

    public void publishWithoutUpdating(
            List<TimeSeriesResponse> timeSeriesResponses,
            FingridPermissionRequest permissionRequest
    ) {
        var id = new IdentifiableValidatedHistoricalData(permissionRequest, timeSeriesResponses);
        vhds.tryEmitNext(id);
    }

    public void publish(CustomerDataResponse data, FingridPermissionRequest permissionRequest) {
        var ap = new IdentifiableAccountingPointData(permissionRequest, data);
        aps.tryEmitNext(ap);
    }

    @Override
    public Flux<RawDataMessage> getRawDataStream() {
        return getIdentifiableValidatedHistoricalDataStream()
                .cast(IdentifiablePayload.class)
                .mergeWith(getIdentifiableAccountingPointDataStream())
                .mapNotNull(id -> {
                    try {
                        var permissionRequest = id.permissionRequest();
                        return new RawDataMessage(permissionRequest, objectMapper.writeValueAsString(id.payload()));
                    } catch (Exception e) {
                        LOGGER.error("Error while serializing {} data", id.payload(), e);
                        return null;
                    }
                });
    }

    @Override
    public void close() {
        rawData.tryEmitComplete();
        vhds.tryEmitComplete();
        aps.tryEmitComplete();
    }

    public Flux<IdentifiableValidatedHistoricalData> getIdentifiableValidatedHistoricalDataStream() {
        return vhds.asFlux();
    }

    public Flux<IdentifiableAccountingPointData> getIdentifiableAccountingPointDataStream() {
        return aps.asFlux();
    }

    @SuppressWarnings("java:S3655") // False positive
    private void publishLatestMeterReading(
            FingridPermissionRequest permissionRequest,
            List<TimeSeriesResponse> timeSeriesResponses
    ) {
        var readings = new HashMap<String, ZonedDateTime>();
        for (var timeSeriesResponse : timeSeriesResponses) {
            var timeSeries = timeSeriesResponse.data().transaction().timeSeries();
            if (timeSeries == null) {
                return;
            }
            var series = timeSeries.getLast();
            var lastObservation = series.observations().getLast();
            var end = lastObservation.start().plusMinutes(series.resolutionDuration().minutes());
            var meterEAN = series.meteringPointEAN();
            var oldMeterReadingEnd = permissionRequest.latestMeterReading(meterEAN);
            boolean isUpdated = oldMeterReadingEnd
                    .map(z -> z.isBefore(end))
                    .orElse(true);
            if (isUpdated) {
                readings.put(meterEAN, end);
            } else {
                readings.put(meterEAN, oldMeterReadingEnd.get());
            }
        }
        outbox.commit(new MeterReadingEvent(permissionRequest.permissionId(), readings));
    }
}
