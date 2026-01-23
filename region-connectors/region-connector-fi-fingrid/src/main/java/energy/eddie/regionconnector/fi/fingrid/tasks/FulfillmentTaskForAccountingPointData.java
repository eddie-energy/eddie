// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.tasks;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fi.fingrid.permission.events.SimpleEvent;
import energy.eddie.regionconnector.fi.fingrid.services.EnergyDataService;
import energy.eddie.regionconnector.fi.fingrid.services.cim.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FulfillmentTaskForAccountingPointData {

    private static final Logger LOGGER = LoggerFactory.getLogger(FulfillmentTaskForAccountingPointData.class);
    private final Outbox outbox;

    public FulfillmentTaskForAccountingPointData(EnergyDataService energyDataService, Outbox outbox) {
        this.outbox = outbox;
        energyDataService.getIdentifiableAccountingPointDataStream()
                         .subscribe(this::onAccountingPointData);
    }

    private void onAccountingPointData(IdentifiableAccountingPointData identifiableAccountingPointData) {
        var permissionId = identifiableAccountingPointData.permissionRequest().permissionId();
        outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.FULFILLED));
        LOGGER.info("Published accounting point data for permission request {} marking it as fulfilled", permissionId);
    }
}
