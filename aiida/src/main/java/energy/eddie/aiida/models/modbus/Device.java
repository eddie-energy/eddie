// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.modbus;

import java.util.UUID;

public record Device(UUID id, String name, UUID modelId) {

    public Device(String id, String name, String modelId) {
        this(UUID.fromString(id), name, UUID.fromString(modelId));
    }
}
