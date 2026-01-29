// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.modbus;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IntervalConfig(
        @JsonProperty("default") long defaultInterval,
        @JsonProperty("min") long minInterval
) {
}