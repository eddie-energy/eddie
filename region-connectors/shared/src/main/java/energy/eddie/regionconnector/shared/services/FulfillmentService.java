// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.services;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.PermissionEventFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

public class FulfillmentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FulfillmentService.class);
    private final Outbox outbox;
    private final PermissionEventFactory permissionEventCtor;

    public FulfillmentService(Outbox outbox, PermissionEventFactory permissionEventCtor) {
        this.outbox = outbox;
        this.permissionEventCtor = permissionEventCtor;
    }


    public void tryFulfillPermissionRequest(PermissionRequest permissionRequest) {
        LOGGER.atInfo()
              .addArgument(() -> permissionRequest.dataSourceInformation().regionConnectorId())
              .addArgument(permissionRequest::permissionId)
              .log("{}: Fulfilling permission request {}");
        outbox.commit(permissionEventCtor.create(permissionRequest.permissionId(),
                                                 PermissionProcessStatus.FULFILLED));
        LOGGER.atInfo()
              .addArgument(() -> permissionRequest.dataSourceInformation().regionConnectorId())
              .addArgument(permissionRequest::permissionId)
              .log("{}: Permission request {} fulfilled");
    }

    public boolean isPermissionRequestFulfilledByDate(PermissionRequest permissionRequest, LocalDate date) {
        return date.isAfter(permissionRequest.end());
    }
}
