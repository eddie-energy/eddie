// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.regionconnector.simulation.providers.DocumentStreams;
import org.springframework.stereotype.Component;

@Component
public class SimulationConnector implements RegionConnector {
    private final DocumentStreams streams;

    public SimulationConnector(DocumentStreams streams) {this.streams = streams;}

    @Override
    public RegionConnectorMetadata getMetadata() {
        return SimulationConnectorMetadata.getInstance();
    }

    @Override
    public void terminatePermission(String permissionId) {
        streams.publish(permissionId);
    }
}