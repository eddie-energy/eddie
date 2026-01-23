// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrSimpleEvent;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Component
public class EnedisRegionConnector implements RegionConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnedisRegionConnector.class);
    private final FrPermissionRequestRepository repository;
    private final Outbox outbox;

    public EnedisRegionConnector(
            FrPermissionRequestRepository repository,
            Outbox outbox
    ) {
        this.repository = repository;
        this.outbox = outbox;
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return EnedisRegionConnectorMetadata.getInstance();
    }

    @Override
    public void terminatePermission(String permissionId) {
        LOGGER.info("{} got termination request for permission {}", REGION_CONNECTOR_ID, permissionId);
        var permissionRequest = repository.findByPermissionId(permissionId);
        if (permissionRequest.isEmpty() || permissionRequest.get().status() != PermissionProcessStatus.ACCEPTED) {
            return;
        }
        outbox.commit(new FrSimpleEvent(permissionId, PermissionProcessStatus.TERMINATED));
    }
}
