package energy.eddie.regionconnector.fi.fingrid.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0_82.ValidatedHistoricalDataEnvelopeProvider;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.regionconnector.fi.fingrid.client.model.CustomerDataResponse;
import energy.eddie.regionconnector.fi.fingrid.client.model.TimeSeriesResponse;
import energy.eddie.regionconnector.fi.fingrid.permission.events.MeterReadingEvent;
import energy.eddie.regionconnector.fi.fingrid.permission.events.SimpleEvent;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequest;
import energy.eddie.regionconnector.shared.cim.v0_82.vhd.VhdEnvelope;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

@Service
public class EnergyDataService implements RawDataProvider, ValidatedHistoricalDataEnvelopeProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnergyDataService.class);
    private final Sinks.Many<RawDataMessage> rawData = Sinks.many().multicast().onBackpressureBuffer();
    private final ObjectMapper objectMapper;
    private final Outbox outbox;
    private final Sinks.Many<ValidatedHistoricalDataEnvelope> vhds = Sinks.many()
                                                                          .multicast()
                                                                          .onBackpressureBuffer();

    public EnergyDataService(
            ObjectMapper objectMapper,
            Outbox outbox
    ) {
        this.objectMapper = objectMapper;
        this.outbox = outbox;
    }

    public Consumer<List<TimeSeriesResponse>> publish(FingridPermissionRequest permissionRequest) {
        return response -> publish(response, permissionRequest);
    }

    public void publish(List<TimeSeriesResponse> timeSeriesResponses, FingridPermissionRequest permissionRequest) {
        publishLatestMeterReading(permissionRequest, timeSeriesResponses);
        publishRawData(timeSeriesResponses, permissionRequest);
        publishValidatedHistoricalDataMarketDocument(timeSeriesResponses, permissionRequest);
    }

    public void publish(CustomerDataResponse data, FingridPermissionRequest permissionRequest) {
        publishRawData(data, permissionRequest);
        publishAccountingPointDataMarketDocument(data, permissionRequest);
        var permissionId = permissionRequest.permissionId();
        outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.FULFILLED));
        LOGGER.info("Published accounting point data for permission request {} marking it as fulfilled", permissionId);
    }

    @Override
    public Flux<RawDataMessage> getRawDataStream() {
        return rawData.asFlux();
    }

    @Override
    public void close() {
        rawData.tryEmitComplete();
        vhds.tryEmitComplete();
    }

    @Override
    public Flux<ValidatedHistoricalDataEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return vhds.asFlux();
    }

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
            readings.put(series.meteringPointEAN(), end);
        }
        outbox.commit(new MeterReadingEvent(permissionRequest.permissionId(), readings));
    }

    private void publishRawData(Object response, FingridPermissionRequest permissionRequest) {
        try {
            rawData.tryEmitNext(new RawDataMessage(permissionRequest.permissionId(),
                                                   permissionRequest.connectionId(),
                                                   permissionRequest.dataNeedId(),
                                                   permissionRequest.dataSourceInformation(),
                                                   ZonedDateTime.now(ZoneOffset.UTC),
                                                   objectMapper.writeValueAsString(response)));
        } catch (JsonProcessingException e) {
            LOGGER.error("Error while serializing time series data", e);
        }
    }

    private void publishValidatedHistoricalDataMarketDocument(
            List<TimeSeriesResponse> timeSeriesResponses,
            FingridPermissionRequest permissionRequest
    ) {
        var docs = new IntermediateValidatedHistoricalDataMarketDocument(timeSeriesResponses).toVhds();
        for (var vhd : docs) {
            vhds.tryEmitNext(new VhdEnvelope(vhd, permissionRequest).wrap());
        }
    }

    private void publishAccountingPointDataMarketDocument(
            CustomerDataResponse accountingPointData,
            FingridPermissionRequest permissionRequest
    ) {
        // TODO
    }
}
