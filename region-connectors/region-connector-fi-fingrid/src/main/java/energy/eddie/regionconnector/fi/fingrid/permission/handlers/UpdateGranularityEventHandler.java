// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.permission.handlers;

import energy.eddie.regionconnector.fi.fingrid.permission.events.UpdateGranularityEvent;
import energy.eddie.regionconnector.fi.fingrid.persistence.FiPermissionRequestRepository;
import energy.eddie.regionconnector.fi.fingrid.services.PollingService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class UpdateGranularityEventHandler implements EventHandler<UpdateGranularityEvent> {

    private final FiPermissionRequestRepository repository;
    private final PollingService pollingService;

    public UpdateGranularityEventHandler(
            EventBus eventBus, FiPermissionRequestRepository repository,
            PollingService pollingService
    ) {
        eventBus.filteredFlux(UpdateGranularityEvent.class)
                .subscribe(this::accept);
        this.repository = repository;
        this.pollingService = pollingService;
    }

    @Override
    public void accept(UpdateGranularityEvent permissionEvent) {
        var pr = repository.getByPermissionId(permissionEvent.permissionId());
        pollingService.pollTimeSeriesData(pr, permissionEvent.granularity());
    }
}
