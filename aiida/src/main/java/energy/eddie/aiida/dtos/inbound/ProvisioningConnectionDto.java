// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.inbound;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProvisioningConnectionDto(
        @JsonProperty("host")
        String host,

        @JsonProperty("username")
        String username,

        @JsonProperty("password")
        String password,

        @JsonProperty("topic")
        String topic
) {
    public static ProvisioningConnectionDto empty() {
        return new ProvisioningConnectionDto("", "", "", "");
    }
}
