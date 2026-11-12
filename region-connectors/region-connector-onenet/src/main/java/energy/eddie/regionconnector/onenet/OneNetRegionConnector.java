// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.onenet;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import org.springframework.stereotype.Component;

@Component
public class OneNetRegionConnector implements RegionConnector {
    private final OneNetRegionConnectorMetadata oneNetRegionConnectorMetadata;

    public OneNetRegionConnector(OneNetRegionConnectorMetadata oneNetRegionConnectorMetadata) {this.oneNetRegionConnectorMetadata = oneNetRegionConnectorMetadata;}

    @Override
    public RegionConnectorMetadata getMetadata() {
        return oneNetRegionConnectorMetadata;
    }

    @Override
    public void terminatePermission(String permissionId) {
        throw new UnsupportedOperationException();
    }
}
