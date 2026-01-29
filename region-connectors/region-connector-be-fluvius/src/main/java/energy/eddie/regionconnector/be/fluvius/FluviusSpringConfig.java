// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius;

import energy.eddie.api.agnostic.RegionConnector;
import energy.eddie.regionconnector.be.fluvius.config.FluviusConfiguration;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@RegionConnector(name = FluviusRegionConnectorMetadata.REGION_CONNECTOR_ID)
@EnableConfigurationProperties({FluviusConfiguration.class, FluviusOAuthConfiguration.class})
@EnableScheduling
public class FluviusSpringConfig {
}
