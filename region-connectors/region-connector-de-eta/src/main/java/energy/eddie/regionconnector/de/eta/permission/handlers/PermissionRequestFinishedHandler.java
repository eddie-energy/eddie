// SPDX-FileCopyrightText: 2026 The ETA+ Developers <bilal.sakhawat@etaplus.energy>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.permission.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionCredentialsRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PermissionRequestFinishedHandler implements EventHandler<PermissionEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestFinishedHandler.class);

    private final DePermissionCredentialsRepository repository;

    public PermissionRequestFinishedHandler(EventBus eventBus, DePermissionCredentialsRepository repository) {
        this.repository = repository;
        eventBus.filteredFlux(PermissionProcessStatus.TERMINATED).subscribe(this::accept);
        eventBus.filteredFlux(PermissionProcessStatus.EXTERNALLY_TERMINATED).subscribe(this::accept);
        eventBus.filteredFlux(PermissionProcessStatus.FULFILLED).subscribe(this::accept);
        eventBus.filteredFlux(PermissionProcessStatus.UNFULFILLABLE).subscribe(this::accept);
        eventBus.filteredFlux(PermissionProcessStatus.REVOKED).subscribe(this::accept);
    }

    @Override
    public void accept(PermissionEvent event) {
        LOGGER.atInfo()
              .addArgument(event::permissionId)
              .addArgument(event::status)
              .log("Permission request {} reached status {}, removing credentials");
        repository.deleteByPermissionId(event.permissionId());
    }
}