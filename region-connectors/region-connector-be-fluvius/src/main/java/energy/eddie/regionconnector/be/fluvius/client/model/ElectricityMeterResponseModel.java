package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.util.List;

public record ElectricityMeterResponseModel(
        @JsonProperty("seqNumber") @Nullable Integer seqNumber,
        @JsonProperty("meterID") @Nullable String meterID,
        @JsonProperty("dailyEnergy") @Nullable List<EDailyEnergyItemResponseModel> dailyEnergy,
        @JsonProperty("quarterHourlyEnergy") @Nullable List<EQuarterHourlyEnergyItemResponseModel> quarterHourlyEnergy
) {
}

