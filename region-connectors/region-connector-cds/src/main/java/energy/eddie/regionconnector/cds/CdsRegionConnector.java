// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.regionconnector.cds.permission.events.SimpleEvent;
import energy.eddie.regionconnector.cds.persistence.CdsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CdsRegionConnector implements RegionConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(CdsRegionConnector.class);
    private final CdsRegionConnectorMetadata metadata;
    private final Outbox outbox;
    private final CdsPermissionRequestRepository repository;

    public CdsRegionConnector(
            CdsRegionConnectorMetadata metadata,
            Outbox outbox,
            CdsPermissionRequestRepository repository
    ) {
        this.metadata = metadata;
        this.outbox = outbox;
        this.repository = repository;
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void terminatePermission(String permissionId) {
        var pr = repository.findByPermissionId(permissionId);
        if (pr.isEmpty()) {
            LOGGER.info("Permission request {} not found", permissionId);
            return;
        }
        var permissionRequest = pr.get();
        if(permissionRequest.status() != PermissionProcessStatus.ACCEPTED) {
            LOGGER.info("Permission request {} not accepted", permissionId);
            return;
        }
        LOGGER.info("Terminating permission request {}", permissionId);
        outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.TERMINATED));
        outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION));
    }
}
