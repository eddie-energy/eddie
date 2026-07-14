// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.inbound;

import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundProvisioningType;

import java.util.UUID;

public class ProvisioningTypeNotConfiguredException extends Exception {
    public ProvisioningTypeNotConfiguredException(UUID permissionId, InboundProvisioningType type) {
        super("Provisioning type of permission %s is configured for %s".formatted(permissionId, type.toString()));
    }
}
