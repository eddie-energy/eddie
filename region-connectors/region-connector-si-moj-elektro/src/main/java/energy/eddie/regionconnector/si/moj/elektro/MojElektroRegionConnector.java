// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.si.moj.elektro;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import org.springframework.stereotype.Component;

@Component
public class MojElektroRegionConnector implements RegionConnector {
    private final MojElektroRegionConnectorMetadata metadata;

    public MojElektroRegionConnector(MojElektroRegionConnectorMetadata metadata) {this.metadata = metadata;}

    @Override
    public RegionConnectorMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void terminatePermission(String permissionId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
