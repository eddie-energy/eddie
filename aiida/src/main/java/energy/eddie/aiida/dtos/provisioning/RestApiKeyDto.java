// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.provisioning;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Plaintext API key returned once after an inbound REST credential is rotated.
 *
 * @param apiKey API key that authenticates requests for the latest inbound record.
 */
public record RestApiKeyDto(
        @JsonProperty("apiKey")
        String apiKey
) {}
