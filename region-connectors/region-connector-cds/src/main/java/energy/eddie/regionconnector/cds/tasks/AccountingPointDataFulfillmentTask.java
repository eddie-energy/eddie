// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.tasks;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.permission.events.SimpleEvent;
import energy.eddie.regionconnector.cds.providers.IdentifiableDataStreams;
import energy.eddie.regionconnector.cds.providers.ap.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AccountingPointDataFulfillmentTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountingPointDataFulfillmentTask.class);
    private final Outbox outbox;

    public AccountingPointDataFulfillmentTask(Outbox outbox, IdentifiableDataStreams streams) {
        this.outbox = outbox;
        streams.accountingPointData()
               .subscribe(this::fulfill);
    }

    private void fulfill(IdentifiableAccountingPointData identifiableAccountingPointData) {
        var permissionId = identifiableAccountingPointData.permissionRequest().permissionId();
        LOGGER.info("Fulfilling permission request {}", permissionId);
        outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.FULFILLED));
    }
}

