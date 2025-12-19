package energy.eddie.regionconnector.be.fluvius.tasks;

import energy.eddie.regionconnector.be.fluvius.client.model.EnergyItemResponseModel;
import energy.eddie.regionconnector.be.fluvius.client.model.MeterResponseModel;
import energy.eddie.regionconnector.be.fluvius.dtos.IdentifiableMeteringData;
import energy.eddie.regionconnector.be.fluvius.permission.events.MeterReadingUpdatedEvent;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;
import energy.eddie.regionconnector.be.fluvius.permission.request.MeterReading;
import energy.eddie.regionconnector.be.fluvius.persistence.MeterReadingRepository;
import energy.eddie.regionconnector.be.fluvius.streams.IdentifiableDataStreams;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


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
                var meterId = meter.meterID();
                if (meterId != null && meterReading.meterEan().endsWith(meterId)) {
                    updateMeters(meterReading, meter, permissionRequest);
                }
            }
        }
        meterReadingRepository.saveAllAndFlush(meterReadings);
        outbox.commit(new MeterReadingUpdatedEvent(permissionId, permissionRequest.status()));
    }

    private List<MeterResponseModel> getMeters(IdentifiableMeteringData identifiableMeteringData) {
        var data = identifiableMeteringData.payload().data();
        if (data == null) {
            return List.of();
        }
        List<MeterResponseModel> meters = new ArrayList<>(Objects.requireNonNullElse(data.electricityMeters(),
                                                                                     List.of()));
        List<? extends MeterResponseModel> gasMeterResponseModels = data.gasMeters();
        meters.addAll(Objects.requireNonNullElse(gasMeterResponseModels, List.of()));
        return meters;
    }

    private void updateMeters(
            MeterReading meterReading,
            MeterResponseModel meter,
            FluviusPermissionRequest permissionRequest
    ) {
        var meterEan = meterReading.meterEan();
        var permissionId = meterReading.permissionId();
        LOGGER.debug("Updating meterReading {} for permission request {}", meterEan, permissionId);

        var granularity = permissionRequest.granularity();
        var readings = meter.getByGranularity(granularity);
        if (readings == null) {
            LOGGER.debug("No meter readings for granularity {} found for permission request {}",
                         granularity,
                         permissionId);
            return;
        }
        updateMeterReading(meterReading, getLatest(readings));
    }

    private void updateMeterReading(
            MeterReading meterReading,
            @Nullable ZonedDateTime newMeterReading
    ) {
        if (newMeterReading == null) {
            return;
        }
        var lastMeterReading = meterReading.lastMeterReading();
        if (lastMeterReading == null || newMeterReading.isAfter(lastMeterReading)) {
            meterReading.setLastMeterReading(newMeterReading);
        }
    }

    @Nullable
    private ZonedDateTime getLatest(List<? extends EnergyItemResponseModel<?>> energyItems) {
        ZonedDateTime lastMeterReading = null;
        for (EnergyItemResponseModel<?> energyItem : energyItems) {
            if (lastMeterReading == null || energyItem.timestampEnd().isAfter(lastMeterReading)) {
                lastMeterReading = energyItem.timestampEnd();
            }
        }
        return lastMeterReading;
    }
}
