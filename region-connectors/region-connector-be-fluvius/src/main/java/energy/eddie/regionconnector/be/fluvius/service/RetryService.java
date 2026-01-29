// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.service;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.be.fluvius.permission.events.ValidatedEvent;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class RetryService {
    private final BePermissionRequestRepository repository;
    private final Outbox outbox;

    public RetryService(BePermissionRequestRepository repository, Outbox outbox) {
        this.repository = repository;
        this.outbox = outbox;
    }

    @Scheduled(cron = "${region-connector.be.fluvius.retry:0 0 * * * *}")
    public void retry() {
        var permissionRequests = repository.findByStatus(PermissionProcessStatus.UNABLE_TO_SEND);
        for (var permissionRequest : permissionRequests) {
            outbox.commit(new ValidatedEvent(
                    permissionRequest.permissionId(),
                    permissionRequest.start(),
                    permissionRequest.end(),
                    permissionRequest.granularity(),
                    permissionRequest.flow()
            ));
        }
    }
}
