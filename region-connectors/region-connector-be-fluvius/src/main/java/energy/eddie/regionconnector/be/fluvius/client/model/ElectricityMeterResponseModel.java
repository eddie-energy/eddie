// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.Granularity;
import jakarta.annotation.Nullable;

import java.util.List;

public record ElectricityMeterResponseModel(
        @JsonProperty("seqNumber") @Nullable Integer seqNumber,
        @JsonProperty("meterID") @Nullable String meterID,
        @JsonProperty("dailyEnergy") @Nullable List<EDailyEnergyItemResponseModel> dailyEnergy,
        @JsonProperty("quarterHourlyEnergy") @Nullable List<EQuarterHourlyEnergyItemResponseModel> quarterHourlyEnergy
) implements MeterResponseModel {

    @Nullable
    @Override
    public List<? extends EnergyItemResponseModel<? extends MeasurementResponseModel>> getByGranularity(Granularity granularity) {
        return switch (granularity) {
            case P1D -> dailyEnergy;
            case PT15M -> quarterHourlyEnergy;
            default -> null;
        };
    }
}