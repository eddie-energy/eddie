// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.us.green.button.permission.events.UsSimpleEvent;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class RetryTerminationService {
    private final UsPermissionRequestRepository repository;
    private final Outbox outbox;

    public RetryTerminationService(UsPermissionRequestRepository repository, Outbox outbox) {
        this.repository = repository;
        this.outbox = outbox;
    }

    @Scheduled(cron = "${region-connector.us.green.button.termination.retry:0 0 * * * *}")
    public void retryTermination() {
        var prs = repository.findAllByStatus(PermissionProcessStatus.FAILED_TO_TERMINATE);
        for (var pr : prs) {
            outbox.commit(new UsSimpleEvent(pr.permissionId(), PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION));
        }
    }
}
