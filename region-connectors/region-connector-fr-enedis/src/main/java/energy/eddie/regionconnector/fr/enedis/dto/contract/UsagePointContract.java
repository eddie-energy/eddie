// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.contract;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UsagePointContract(
        @JsonProperty("usage_point")
        UsagePoint usagePoint,
        @JsonProperty("contracts")
        Contract contract
) {
}
