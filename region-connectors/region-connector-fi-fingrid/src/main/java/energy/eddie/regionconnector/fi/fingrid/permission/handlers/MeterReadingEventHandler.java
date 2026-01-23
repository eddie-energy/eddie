// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.permission.handlers;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.fi.fingrid.permission.events.MeterReadingEvent;
import energy.eddie.regionconnector.fi.fingrid.permission.events.SimpleEvent;
import energy.eddie.regionconnector.fi.fingrid.persistence.FiPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.endOfDay;

@Component
public class MeterReadingEventHandler implements EventHandler<MeterReadingEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeterReadingEventHandler.class);
    private final DataNeedCalculationService<DataNeed> calculationService;
    private final FiPermissionRequestRepository repository;
    private final Outbox outbox;

    public MeterReadingEventHandler(
            DataNeedCalculationService<DataNeed> calculationService,
            FiPermissionRequestRepository repository,
            Outbox outbox,
            EventBus eventBus
    ) {
        this.calculationService = calculationService;
        this.repository = repository;
        this.outbox = outbox;
        eventBus.filteredFlux(MeterReadingEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(MeterReadingEvent permissionEvent) {
        var permissionId = permissionEvent.permissionId();
        var pr = repository.getByPermissionId(permissionId);
        var dataNeedId = pr.dataNeedId();
        var calc = calculationService.calculate(dataNeedId, pr.created());
        if (!(calc instanceof ValidatedHistoricalDataDataNeedResult result)) {
            LOGGER.warn("Got unexpected data need {} for permission request {}", dataNeedId, permissionId);
            return;
        }
        var end = endOfDay(result.energyTimeframe().end(), ZoneOffset.UTC);
        var isFulfilled = permissionEvent.lastMeterReadings()
                                         .values()
                                         .stream()
                                         .noneMatch(reading -> reading.isBefore(end));
        if (isFulfilled) {
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.FULFILLED));
        }
    }
}
