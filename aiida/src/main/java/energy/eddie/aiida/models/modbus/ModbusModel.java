// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.modbus;

import java.util.UUID;

public record ModbusModel(
        UUID id,
        String name,
        UUID vendorId
) {
    public ModbusModel(String id, String name, String vendorId) {
        this(UUID.fromString(id), name, UUID.fromString(vendorId));
    }
}


