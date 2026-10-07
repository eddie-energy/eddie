// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.subscription;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;

/**
 * Request body of the subscribed services API ({@code POST /subscribed_services/v1}).
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ServiceSubscription(
        @JsonProperty("pointId") @Nullable List<String> pointId,
        @JsonProperty("siren") @Nullable List<String> siren,
        @JsonProperty("dateDebut") @Nullable LocalDate start,
        @JsonProperty("dateFin") @Nullable LocalDate end,
        @JsonProperty("etatCode") @Nullable List<ServiceState> serviceStates,
        @JsonProperty("serviceType") @Nullable ServiceType serviceType,
        @JsonProperty("mesureTypeCode") @Nullable List<MeasureType> measureTypes,
        @JsonProperty("soutirage") @Nullable Boolean consumption,
        @JsonProperty("injection") @Nullable Boolean injection,
        @JsonProperty("page") @Nullable Integer page,
        @JsonProperty("comptage") boolean count,
        @JsonProperty("autorisationId") @Nullable Long authorizationId,
        @JsonProperty("autorisation") @Nullable Boolean authorization
) {
    /**
     * Creates the minimal query used by the Data Connect flow to resolve the
     * usage point ids of an authorization.
     *
     * @param authorizationId Identifier of the authorization given in the consent callback.
     */
    public ServiceSubscription(long authorizationId) {
        this(null, null, null, null, null, ServiceType.ACCES, null, null, null, null, false, authorizationId, true);
    }
}
