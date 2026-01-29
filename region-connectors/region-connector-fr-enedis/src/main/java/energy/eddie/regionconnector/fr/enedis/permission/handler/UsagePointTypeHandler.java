// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.permission.handler;

import energy.eddie.regionconnector.fr.enedis.permission.events.FrUsagePointTypeEvent;
import energy.eddie.regionconnector.fr.enedis.services.HistoricalDataService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class UsagePointTypeHandler implements EventHandler<FrUsagePointTypeEvent> {
    private final HistoricalDataService historicalDataService;

    public UsagePointTypeHandler(HistoricalDataService historicalDataService, EventBus eventBus) {
        this.historicalDataService = historicalDataService;
        eventBus.filteredFlux(FrUsagePointTypeEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(FrUsagePointTypeEvent permissionEvent) {
        var permissionId = permissionEvent.permissionId();
        historicalDataService.fetchHistoricalMeterReadings(permissionId);
    }
}
