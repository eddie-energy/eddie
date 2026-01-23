// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.permission.handler;

import energy.eddie.regionconnector.fr.enedis.permission.events.FrAcceptedEvent;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import energy.eddie.regionconnector.fr.enedis.services.AccountingPointDataService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class AcceptedHandler implements EventHandler<FrAcceptedEvent> {
    private final AccountingPointDataService accountingPointDataService;
    private final FrPermissionRequestRepository repository;

    public AcceptedHandler(
            AccountingPointDataService accountingPointDataService,
            EventBus eventBus, FrPermissionRequestRepository repository
    ) {
        this.accountingPointDataService = accountingPointDataService;
        this.repository = repository;
        eventBus.filteredFlux(FrAcceptedEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(FrAcceptedEvent event) {
        final String usagePointId = event.usagePointId();
        var request = repository.getByPermissionId(event.permissionId());

        // no granularity means accounting point data needs to be fetched
        if (request.granularity() == null) {
            accountingPointDataService.fetchAccountingPointData(request, usagePointId);
        } else {
            accountingPointDataService.fetchMeteringPointSegment(event.permissionId(), usagePointId);
        }
    }
}
