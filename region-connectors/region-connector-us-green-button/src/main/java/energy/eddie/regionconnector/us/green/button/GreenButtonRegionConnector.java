// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.us.green.button.permission.events.UsSimpleEvent;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GreenButtonRegionConnector implements RegionConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(GreenButtonRegionConnector.class);
    private final Outbox outbox;
    private final UsPermissionRequestRepository repository;

    public GreenButtonRegionConnector(Outbox outbox, UsPermissionRequestRepository repository) {
        this.outbox = outbox;
        this.repository = repository;
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return GreenButtonRegionConnectorMetadata.getInstance();
    }

    @Override
    public void terminatePermission(String permissionId) {
        var exists = repository.existsByPermissionIdAndStatus(permissionId, PermissionProcessStatus.ACCEPTED);
        if (exists) {
            outbox.commit(new UsSimpleEvent(permissionId, PermissionProcessStatus.TERMINATED));
            outbox.commit(new UsSimpleEvent(permissionId, PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION));
        } else {
            LOGGER.warn(
                    "Permission request {} does not exist or does not have the accepted status and cannot be terminated",
                    permissionId
            );
        }
    }
}
