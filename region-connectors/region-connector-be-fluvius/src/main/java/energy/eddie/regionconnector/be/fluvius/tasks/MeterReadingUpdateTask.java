// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.tasks;

import energy.eddie.regionconnector.be.fluvius.dtos.IdentifiableMeteringData;
import energy.eddie.regionconnector.be.fluvius.permission.events.MeterReadingUpdatedEvent;
import energy.eddie.regionconnector.be.fluvius.permission.request.MeterReading;
import energy.eddie.regionconnector.be.fluvius.persistence.MeterReadingRepository;
import energy.eddie.regionconnector.be.fluvius.streams.IdentifiableDataStreams;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;


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
        var headpoint = identifiableMeteringData.payload().data().headpoint();
        if (headpoint == null) {
            LOGGER.info("No headpoint present in one of the energy data payloads for permission request {}",
                        permissionId);
            return;
        }
        var latestMeterReading = headpoint.getLatestMeterReading(identifiableMeteringData.permissionRequest()
                                                                                         .granularity());
        if (latestMeterReading == null) {
            LOGGER.info("No meter reading present in payload for permission request {}", permissionId);
            return;
        }
        for (var meterReading : meterReadings) {
            if (meterReading.meterEan().equals(headpoint.ean())) {
                updateMeters(meterReading, latestMeterReading);
            }
        }
        meterReadingRepository.saveAllAndFlush(meterReadings);
        outbox.commit(new MeterReadingUpdatedEvent(permissionId, permissionRequest.status()));
    }

    private void updateMeters(MeterReading meterReading, ZonedDateTime latestReading) {
        var meterEan = meterReading.meterEan();
        var permissionId = meterReading.permissionId();
        LOGGER.debug("Updating meterReading {} for permission request {}", meterEan, permissionId);
        var lastMeterReading = meterReading.lastMeterReading();
        if (latestReading.isAfter(lastMeterReading)) {
            meterReading.setLastMeterReading(latestReading);
        }
    }
}
