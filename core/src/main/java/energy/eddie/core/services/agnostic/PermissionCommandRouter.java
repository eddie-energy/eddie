// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.services.agnostic;

import energy.eddie.api.agnostic.command.RegionConnectorPermissionCommandService;
import energy.eddie.api.agnostic.outbound.PermissionCommandOutboundConnector;
import energy.eddie.cim.agnostic.PermissionCommand;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This service routes permission commands between the region connectors. It does that by using the regionConnectorId.
 */
@Service
public class PermissionCommandRouter implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCommandRouter.class);
    private final Map<String, RegionConnectorPermissionCommandService> permissionCommandServices = new HashMap<>();

    private final List<Disposable> subscriptions = new ArrayList<>();

    public void registerPermissionCommandService(
            String regionConnectorId,
            RegionConnectorPermissionCommandService permissionCommandService
    ) {
        LOGGER.info("{}: Registering RegionConnectorPermissionCommandService: {}",
                    regionConnectorId,
                    permissionCommandService.getClass().getName());
        permissionCommandServices.put(regionConnectorId, permissionCommandService);
    }

    public void registerPermissionCommandConnector(PermissionCommandOutboundConnector permissionCommandOutboundConnector) {
        LOGGER.info("Registering PermissionCommandOutboundConnector: {}",
                    permissionCommandOutboundConnector.getClass().getName());
        var subscription = permissionCommandOutboundConnector.getPermissionCommands()
                                                             .subscribe(
                                                                     this::route,
                                                                     e -> LOGGER.error(
                                                                             "Error in PermissionCommandRouter", e
                                                                     ));
        subscriptions.add(subscription);
    }

    @Override
    public void close() throws Exception {
        for (var subscription : subscriptions) {
            subscription.dispose();
        }
    }

    private void route(PermissionCommand permissionCommand) {
        var regionConnectorId = permissionCommand.regionConnectorId();
        LOGGER.info("Will route PermissionCommand for region connector ID {}", regionConnectorId);

        if (!routeIfServicePresent(regionConnectorId, permissionCommand)) {
            LOGGER.warn("Could not find region connector with id {}", regionConnectorId);
        }
    }


    private boolean routeIfServicePresent(@Nullable String key, PermissionCommand permissionCommand) {
        var rc = permissionCommandServices.get(key);
        if (rc == null) {
            return false;
        }
        rc.permissionCommandArrived(permissionCommand);
        return true;
    }
}
