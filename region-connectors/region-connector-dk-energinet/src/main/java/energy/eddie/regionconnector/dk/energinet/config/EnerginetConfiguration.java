// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;

/**
 * The main configuration for the Energinet region connector.
 *
 * @param customerBasePath BasePath for the customer api
 */
@ConfigurationProperties("region-connector.dk.energinet")
public record EnerginetConfiguration(
        @Name("customer.client.basepath") String customerBasePath
) {}
