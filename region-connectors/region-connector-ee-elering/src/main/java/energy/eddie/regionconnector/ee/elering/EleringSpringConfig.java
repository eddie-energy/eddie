// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.ee.elering;

import energy.eddie.api.agnostic.RegionConnector;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static energy.eddie.regionconnector.ee.elering.EleringRegionConnectorMetadata.REGION_CONNECTOR_ID;

@RegionConnector(name = REGION_CONNECTOR_ID)
@SpringBootApplication
public class EleringSpringConfig {
}
