// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.identity;

import com.fasterxml.jackson.annotation.JsonProperty;


public record LegalEntity(
        @JsonProperty("name")
        String name,
        @JsonProperty("trading_name")
        String tradingName,
        @JsonProperty("business")
        String business,
        @JsonProperty("industry")
        String industry,
        @JsonProperty("siret_number")
        String siretNumber
) {
}
