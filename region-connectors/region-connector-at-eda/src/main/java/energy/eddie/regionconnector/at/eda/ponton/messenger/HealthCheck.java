// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messenger;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HealthCheck(
        @JsonProperty("name")
        String name,
        @JsonProperty("ok")
        boolean ok,
        @JsonProperty("content")
        String content
) {
}
