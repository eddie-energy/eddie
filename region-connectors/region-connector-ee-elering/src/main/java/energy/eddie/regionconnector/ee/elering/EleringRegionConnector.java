// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.ee.elering;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import org.springframework.stereotype.Component;

@Component
public class EleringRegionConnector implements RegionConnector {

    private final EleringRegionConnectorMetadata metadata;

    public EleringRegionConnector(EleringRegionConnectorMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void terminatePermission(String permissionId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
