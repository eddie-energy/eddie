// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.regionconnector.be.fluvius.service.TerminationService;
import org.springframework.stereotype.Component;

@Component
public class FluviusRegionConnector implements RegionConnector {
    private final TerminationService terminationService;
    private final FluviusRegionConnectorMetadata metadata;

    public FluviusRegionConnector(TerminationService terminationService, FluviusRegionConnectorMetadata metadata) {
        this.terminationService = terminationService;
        this.metadata = metadata;
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void terminatePermission(String permissionId) {
        terminationService.terminate(permissionId);
    }
}
