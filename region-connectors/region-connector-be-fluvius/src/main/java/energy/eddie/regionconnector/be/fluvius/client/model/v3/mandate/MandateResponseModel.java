// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model.v3.mandate;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.EnergyType;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;

public record MandateResponseModel(
        @JsonProperty("referenceNumber") @Nullable String referenceNumber,
        @JsonProperty("status") @Nullable Status status,
        @JsonProperty("eanNumber") @Nullable String eanNumber,
        @JsonProperty("energyType") @Nullable EnergyType energyType,
        @JsonProperty("dataPeriodFrom") @Nullable ZonedDateTime dataPeriodFrom,
        @JsonProperty("dataPeriodTo") @Nullable ZonedDateTime dataPeriodTo,
        @JsonProperty("dataServiceType") @Nullable DataServiceType dataServiceType,
        @JsonProperty("mandateExpirationDate") @Nullable ZonedDateTime mandateExpirationDate,
        @JsonProperty("renewalStatus") @Nullable RenewalStatus renewalStatus
) {
    public boolean supportsGranularity(Granularity granularity) {
        return switch (dataServiceType) {
            case HOURLY_OR_QUARTER_HOURLY -> granularity == Granularity.PT15M || granularity == Granularity.PT1H;
            case DAILY -> granularity == Granularity.P1D;
            case null, default -> false;
        };
    }

    public boolean supportsEnergyType(energy.eddie.api.agnostic.data.needs.EnergyType energyType) {
        return switch (this.energyType) {
            case ELECTRICITY -> energy.eddie.api.agnostic.data.needs.EnergyType.ELECTRICITY == energyType;
            case GAS -> energy.eddie.api.agnostic.data.needs.EnergyType.NATURAL_GAS == energyType;
            case null -> false;
        };
    }

    public enum Status {
        @JsonProperty("Requested")
        REQUESTED,
        @JsonProperty("Approved")
        APPROVED,
        @JsonProperty("Rejected")
        REJECTED,
        @JsonProperty("Finished")
        FINISHED;
    }

    /**
     * Defines the available data service types.
     * <p>
     * <i>Members</i>:
     * <ul>
     *     <li>
     *         <i>VH_onbepaald</i> - Consumption history with undefined granularity.
     *     </li>
     *     <li>
     *         <i>VH_kwartier_uur</i> - Consumption history with granularity hour or quarter-hour.
     *     </li>
     *     <li>
     *         <i>VH_dag</i> - Consumption history with granularity day.
     *     </li>
     *     <li>
     *         <i>IG</i> - Installation data.
     *     </li>
     * </ul>
     */
    public enum DataServiceType {
        @JsonProperty("VH_onbepaald")
        @JsonEnumDefaultValue
        UNDEFINED,
        @JsonProperty("VH_kwartier_uur")
        HOURLY_OR_QUARTER_HOURLY,
        @JsonProperty("VH_dag")
        DAILY,
        @JsonProperty("IG")
        INSTALLATION;
    }

    public enum RenewalStatus {
        @JsonProperty("ToBeRenewed")
        TO_BE_RENEWED,
        @JsonProperty("RenewalRequested")
        RENEWAL_REQUESTED,
        @JsonProperty("Expired")
        EXPIRED;
    }
}