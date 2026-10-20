// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.situation;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

/**
 * One element of the situation contractuelle API response
 * ({@code GET /situation_contrat_auto/v1/{usage_point_id}}), which returns an array of these.
 * <p>
 * Shaped from the real response, where {@code contact_data}, {@code person} and {@code organization}
 * are siblings of {@code customer} rather than nested inside it.
 */
public record ContractualSituation(
        @JsonProperty("usage_point_id") @Nullable String usagePointId,
        @JsonProperty("contract_start") @Nullable String contractStart,
        @JsonProperty("contract_type") @Nullable String contractType,
        @JsonProperty("contractor") @Nullable String contractor,
        @JsonProperty("balance_responsable_party") @Nullable String balanceResponsibleParty,
        @JsonProperty("pricing_structure") @Nullable String pricingStructure,
        @JsonProperty("distribution_tariff") @Nullable String distributionTariff,
        @JsonProperty("supplier_tariff_profile") @Nullable String supplierTariffProfile,
        @JsonProperty("distribution_tariff_profile") @Nullable String distributionTariffProfile,
        @JsonProperty("supplier_mobile_peak") @Nullable String supplierMobilePeak,
        @JsonProperty("distribution_mobile_peak") @Nullable String distributionMobilePeak,
        @JsonProperty("subscribed_power")
        @JsonDeserialize(using = SubscribedPowerDeserializer.class)
        @Nullable String subscribedPower,
        @JsonProperty("segment")
        @JsonDeserialize(using = SegmentListDeserializer.class)
        @Nullable List<String> segments,
        @JsonProperty("customer") @Nullable Customer customer,
        @JsonProperty("contact_data") @Nullable ContactData contactData,
        @JsonProperty("person") @Nullable Person person,
        @JsonProperty("organization") @Nullable Organization organization
) {
}