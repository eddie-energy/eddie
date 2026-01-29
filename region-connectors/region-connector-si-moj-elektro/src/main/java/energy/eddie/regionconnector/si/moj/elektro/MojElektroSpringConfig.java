// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.si.moj.elektro;

import energy.eddie.api.agnostic.RegionConnector;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static energy.eddie.regionconnector.si.moj.elektro.MojElektroRegionConnectorMetadata.REGION_CONNECTOR_ID;

@RegionConnector(name = REGION_CONNECTOR_ID)
@SpringBootApplication
public class MojElektroSpringConfig {
}
