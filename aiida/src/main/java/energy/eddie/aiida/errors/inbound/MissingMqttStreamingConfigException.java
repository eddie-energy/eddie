// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.inbound;

import jakarta.annotation.Nullable;

import java.util.UUID;

public class MissingMqttStreamingConfigException extends RuntimeException {
    public MissingMqttStreamingConfigException(@Nullable UUID dataSourceId) {
        super(dataSourceId == null
                      ? "MQTT streaming configuration is missing for an unpersisted inbound data source"
                      : "MQTT streaming configuration is missing for inbound data source " + dataSourceId);
    }
}
