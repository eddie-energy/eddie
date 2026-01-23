// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.client.dtos.authorization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Scope(@JsonProperty String expires, @JsonProperty("ongoing_end") @Nullable ZonedDateTime ongoingEnd) {
}
