package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.util.List;

public record GasMeterResponseModel(
        @JsonProperty("seqNumber") @Nullable Integer seqNumber,
        @JsonProperty("meterID") @Nullable String meterID,
        @JsonProperty("dailyEnergy") @Nullable List<GDailyEnergyItemResponseModel> dailyEnergy,
        @JsonProperty("hourlyEnergy") @Nullable List<GHourlyEnergyItemResponseModel> hourlyEnergy
) {}

