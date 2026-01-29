// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.timeout;


import energy.eddie.api.agnostic.process.model.persistence.StalePermissionRequestRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.PermissionEventFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonTimeoutService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonTimeoutService.class);
    private final StalePermissionRequestRepository<?> repository;
    private final PermissionEventFactory factory;
    private final Outbox outbox;
    private final TimeoutConfiguration config;
    private final RegionConnectorMetadata metadata;

    public CommonTimeoutService(
            StalePermissionRequestRepository<?> repository,
            PermissionEventFactory factory,
            Outbox outbox,
            TimeoutConfiguration config,
            RegionConnectorMetadata metadata
    ) {
        this.repository = repository;
        this.factory = factory;
        this.outbox = outbox;
        this.config = config;
        this.metadata = metadata;
    }

    /**
     * Times out all permission requests returned by the {@link StalePermissionRequestRepository}.
     * Depending on the status of the permission request, different events are emitted.
     * If the permission request has the status {@code VALIDATED} an event with the status {@code SENT_TO_PERMISSION_ADMINISTRATOR} and another event with the status {@code TIMED_OUT} are emitted.
     * If the permission request has the status {@code SENT_TO_PERMISSION_ADMINISTRATOR} only the timed-out event is emitted.
     */
    @Timeout
    public void timeout() {
        LOGGER.info("Querying permission requests to timeout");
        var duration = config.duration();
        var permissionRequests = repository.findStalePermissionRequests(duration);
        LOGGER.atInfo()
              .addArgument(metadata::id)
              .addArgument(permissionRequests::size)
              .addArgument(duration)
              .log("{}: Found {} permission requests that have been stale for the last {} hours, starting timeout.");
        for (var pr : permissionRequests) {
            var permissionId = pr.permissionId();
            LOGGER.atInfo()
                  .addArgument(metadata::id)
                  .addArgument(permissionId)
                  .log("{}: Timing out permission request {}");
            if (pr.status() == PermissionProcessStatus.VALIDATED) {
                outbox.commit(factory.create(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
            }
            outbox.commit(factory.create(permissionId, PermissionProcessStatus.TIMED_OUT));
        }
    }
}
