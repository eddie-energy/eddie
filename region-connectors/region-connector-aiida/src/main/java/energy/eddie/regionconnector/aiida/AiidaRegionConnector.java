// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.regionconnector.aiida.services.AiidaPermissionService;
import org.springframework.stereotype.Component;

@Component
public class AiidaRegionConnector implements RegionConnector {
    private final AiidaPermissionService aiidaPermissionService;

    public AiidaRegionConnector(
            AiidaPermissionService aiidaPermissionService
    ) {
        this.aiidaPermissionService = aiidaPermissionService;
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return AiidaRegionConnectorMetadata.getInstance();
    }

    @Override
    public void terminatePermission(String permissionId) {
        aiidaPermissionService.terminatePermission(permissionId);
    }

}
