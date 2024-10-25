package energy.eddie.regionconnector.fi.fingrid.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.api.v0_82.ValidatedHistoricalDataEnvelopeProvider;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.regionconnector.fi.fingrid.client.TimeSeriesResponse;
import energy.eddie.regionconnector.fi.fingrid.permission.events.MeterReadingEvent;
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

    public Consumer<TimeSeriesResponse> publish(FingridPermissionRequest permissionRequest) {
        return response -> publish(response, permissionRequest);
    }

    public void publish(TimeSeriesResponse timeSeriesResponse, FingridPermissionRequest permissionRequest) {
        publishLatestMeterReading(permissionRequest, timeSeriesResponse);
        publishRawData(timeSeriesResponse, permissionRequest);
        publishValidatedHistoricalDataMarketDocument(timeSeriesResponse, permissionRequest);
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
            TimeSeriesResponse timeSeriesResponse
    ) {
        var timeSeries = timeSeriesResponse.data().transaction().timeSeries();
        if (timeSeries == null) {
            return;
        }
        var series = timeSeries.getLast();
        var lastObservation = series.observations().getLast();
        var end = lastObservation.start().plusMinutes(series.resolutionDuration().minutes());
        outbox.commit(new MeterReadingEvent(permissionRequest.permissionId(), end));
    }

    private void publishRawData(TimeSeriesResponse timeSeriesResponse, FingridPermissionRequest permissionRequest) {
        try {
            rawData.tryEmitNext(new RawDataMessage(permissionRequest.permissionId(),
                                                   permissionRequest.connectionId(),
                                                   permissionRequest.dataNeedId(),
                                                   permissionRequest.dataSourceInformation(),
                                                   ZonedDateTime.now(ZoneOffset.UTC),
                                                   objectMapper.writeValueAsString(timeSeriesResponse)));
        } catch (JsonProcessingException e) {
            LOGGER.error("Error while serializing time series data", e);
        }
    }

    private void publishValidatedHistoricalDataMarketDocument(
            TimeSeriesResponse timeSeriesResponse,
            FingridPermissionRequest permissionRequest
    ) {
        var vhd = new IntermediateValidatedHistoricalDataMarketDocument(timeSeriesResponse);
        vhds.tryEmitNext(new VhdEnvelope(vhd.toVhd(), permissionRequest).wrap());
    }
}
