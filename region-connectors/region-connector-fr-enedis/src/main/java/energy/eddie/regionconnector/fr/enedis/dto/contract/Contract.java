// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.contract;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Contract(
        @JsonProperty("segment")
        String segment,
        @JsonProperty("subscribed_power")
        String subscribedPower,
        @JsonProperty("last_activation_date")
        String lastActivationDate,
        @JsonProperty("distribution_tariff")
        String distributionTariff,
        @JsonProperty("offpeak_hours")
        String offPeakHours,
        @JsonProperty("contract_type")
        String contractType,
        @JsonProperty("contract_status")
        String contractStatus,
        @JsonProperty("last_distribution_tariff_change_date")
        String lastDistributionTariffChangeDate
) {
}
