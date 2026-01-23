// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.regionconnector.fi.fingrid.permission.events.SimpleEvent;
import energy.eddie.regionconnector.fi.fingrid.persistence.FiPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FingridRegionConnector implements RegionConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(FingridRegionConnector.class);
    private final FiPermissionRequestRepository permissionRequestRepository;
    private final Outbox outbox;

    public FingridRegionConnector(FiPermissionRequestRepository permissionRequestRepository, Outbox outbox) {
        this.permissionRequestRepository = permissionRequestRepository;
        this.outbox = outbox;
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return FingridRegionConnectorMetadata.INSTANCE;
    }

    @Override
    public void terminatePermission(String permissionId) {
        var pr = permissionRequestRepository.findByPermissionId(permissionId);
        if (pr.isEmpty()) {
            LOGGER.info("Permission request {} does not exist, cannot terminate", permissionId);
        } else if (pr.get().status() != PermissionProcessStatus.ACCEPTED) {
            LOGGER.info("Permission request {} is not accepted, but has the {} status, cannot terminate",
                        permissionId,
                        pr.get().status());
        } else {
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.TERMINATED));
            LOGGER.info("Terminated permission request {}", permissionId);
        }
    }
}
