// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.tasks;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.permission.events.SimpleEvent;
import energy.eddie.regionconnector.cds.persistence.CdsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RetryTerminationTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetryTerminationTask.class);
    private final CdsPermissionRequestRepository repository;
    private final Outbox outbox;

    public RetryTerminationTask(CdsPermissionRequestRepository repository, Outbox outbox) {
        this.repository = repository;
        this.outbox = outbox;
    }

    @RetrySending
    public void retry() {
        var prs = repository.findByStatus(PermissionProcessStatus.FAILED_TO_TERMINATE);
        LOGGER.atInfo()
              .addArgument(prs::size)
              .log("Retrying externally terminating {} permission requests");
        for (var pr : prs) {
            var permissionId = pr.permissionId();
            LOGGER.info("Retrying externally terminating permission request {}", permissionId);
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION));
        }
    }
}
