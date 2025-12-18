package energy.eddie.regionconnector.be.fluvius.tasks;

import energy.eddie.regionconnector.be.fluvius.client.model.GetEnergyResponseModel;
import energy.eddie.regionconnector.be.fluvius.client.model.GetEnergyResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.client.model.Meter;
import energy.eddie.regionconnector.be.fluvius.client.model.Readings;
import energy.eddie.regionconnector.be.fluvius.dtos.IdentifiableMeteringData;
import energy.eddie.regionconnector.be.fluvius.permission.events.MeterReadingUpdatedEvent;
import energy.eddie.regionconnector.be.fluvius.permission.request.MeterReading;
import energy.eddie.regionconnector.be.fluvius.persistence.MeterReadingRepository;
import energy.eddie.regionconnector.be.fluvius.streams.IdentifiableDataStreams;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;


@Component
public class MeterReadingUpdateTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeterReadingUpdateTask.class);
    private final MeterReadingRepository meterReadingRepository;
    private final Outbox outbox;

    public MeterReadingUpdateTask(
            IdentifiableDataStreams streams,
            MeterReadingRepository meterReadingRepository,
            Outbox outbox
    ) {
        this.meterReadingRepository = meterReadingRepository;
        this.outbox = outbox;
        streams.getMeteringData()
               .subscribe(this::onMeterReading);
    }


    public void onMeterReading(IdentifiableMeteringData identifiableMeteringData) {
        var permissionRequest = identifiableMeteringData.permissionRequest();
        var permissionId = permissionRequest.permissionId();
        LOGGER.info("Got new meter readings for permission request {}", permissionId);
        var meterReadings = meterReadingRepository.findAllByPermissionId(permissionId);
        var meters = getMeters(identifiableMeteringData);
        for (var meterReading : meterReadings) {
            for (var meter : meters) {
                if (meterReading.meterEan().endsWith(meter.getMeterId())) {
                    updateMeters(meterReading, meter);
                }
            }
        }
        meterReadingRepository.saveAllAndFlush(meterReadings);
        outbox.commit(new MeterReadingUpdatedEvent(permissionId, permissionRequest.status()));
    }


    private List<Meter<?>> getMeters(IdentifiableMeteringData identifiableMeteringData) {
        var electricityMeters = get(identifiableMeteringData.payload(), GetEnergyResponseModel::electricityMeters);
        var gasMeters = get(identifiableMeteringData.payload(), GetEnergyResponseModel::gasMeters);
        var meters = new ArrayList<Meter<?>>();
        for (var electricityMeter : electricityMeters) {
            meters.add(Meter.from(electricityMeter));
        }
        for (var gasMeter : gasMeters) {
            meters.add(Meter.from(gasMeter));
        }
        return meters;
    }

    private void updateMeters(
            MeterReading meterReading,
            Meter<?> meter
    ) {
        var meterEan = meterReading.meterEan();
        var permissionId = meterReading.permissionId();
        LOGGER.info("Updating meterReading {} for permission request {}", meterEan, permissionId);

        for (Readings<?> readings : meter.getReadingSources()) {
            if (updateMeterReading(meterReading, readings)) {
                return;
            }
        }
    }

    private boolean updateMeterReading(
            MeterReading meterReading,
            Readings<?> readings
    ) {
        var end = readings.getLastReadingTimestamp();
        end.ifPresent(meterReading::setLastMeterReading);
        return end.isPresent();
    }


    private <T> List<T> get(
            GetEnergyResponseModelApiDataResponse payload,
            Function<GetEnergyResponseModel, List<T>> getter
    ) {
        var data = payload.data();
        if (data == null) {
            return List.of();
        }
        UnaryOperator<List<T>> onNullFunc = res -> Objects.requireNonNullElse(res, List.of());
        return getter.andThen(onNullFunc)
                     .apply(data);
    }
}
