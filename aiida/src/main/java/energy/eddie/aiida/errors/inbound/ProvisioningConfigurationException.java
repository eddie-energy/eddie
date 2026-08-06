// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.inbound;

import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundProvisioningType;

import java.util.UUID;

public class ProvisioningConfigurationException extends Exception {
    public ProvisioningConfigurationException(UUID dataSourceId, InboundProvisioningType provisioningType) {
        super("MQTT provisioning configuration is missing for inbound data source %s configured for %s"
                      .formatted(dataSourceId, provisioningType));
    }
}
