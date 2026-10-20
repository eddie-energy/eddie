// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

/**
 * A single subscribed service as returned by the subscribed services API.
 *
 * @param id                      Identifier of the subscribed service.
 * @param injection               True if the service concerns production (injection) data.
 * @param consumption             True if the service concerns consumption (withdrawal) data.
 * @param serviceType             Code of the service (e.g. {@link ServiceType#ACCES}).
 * @param start                   Start date of the service.
 * @param end                     End date of the service.
 * @param state                   State of the service.
 * @param stateLabel              Label associated with the state.
 * @param pointId                 Identifier of the point (the PRM).
 * @param holderSiren             SIREN of the point holder.
 * @param beneficiarySiren        SIREN of the service beneficiary.
 * @param measureType             Type of measurement (CDC, IDX, PMAX, ENERGIE, ITC).
 * @param measureStep             Measurement step (PT10M, PT15M, PT30M, P1D, NA).
 * @param transmissionPeriodicity Data publication frequency (P1D, P7D, P1M).
 * @param correctedMeasures       Whether the measures are corrected or raw.
 * @param dynamicSpace            True if the service is intended for the dynamic space.
 * @param dataPublication         True if the service is an access service with a publication option.
 * @param authorization           Authorization details associated with the service.
 */
public record SubscribedService(
        @JsonProperty("id") @Nullable String id,
        @JsonProperty("injection") @Nullable Boolean injection,
        @JsonProperty("soutirage") @Nullable Boolean consumption,
        @JsonProperty("serviceCode") @Nullable ServiceType serviceType,
        @JsonProperty("dateDebut") @Nullable LocalDate start,
        @JsonProperty("dateFin") @Nullable LocalDate end,
        @JsonProperty("etatCode") @Nullable ServiceState state,
        @JsonProperty("etatLibelle") @Nullable String stateLabel,
        @JsonProperty("pointId") @Nullable String pointId,
        @JsonProperty("sirenTitulaire") @Nullable String holderSiren,
        @JsonProperty("sirenBeneficiaire") @Nullable String beneficiarySiren,
        @JsonProperty("mesuresTypeCode") @Nullable MeasureType measureType,
        @JsonProperty("mesuresPas") @Nullable String measureStep,
        @JsonProperty("periodiciteTransmission") @Nullable String transmissionPeriodicity,
        @JsonProperty("mesuresCorrigees") @Nullable Boolean correctedMeasures,
        @JsonProperty("espaceDynamique") @Nullable Boolean dynamicSpace,
        @JsonProperty("publicationDonnees") @Nullable Boolean dataPublication,
        @JsonProperty("autorisation") @Nullable ServiceAuthorization authorization
) {
}
