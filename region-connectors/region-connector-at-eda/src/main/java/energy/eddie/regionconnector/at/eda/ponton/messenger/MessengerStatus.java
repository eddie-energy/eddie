// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messenger;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record MessengerStatus(
        @JsonProperty("healthChecks")
        Map<String, HealthCheck> healthChecks,
        @JsonProperty("ok")
        boolean ok
) {
}
