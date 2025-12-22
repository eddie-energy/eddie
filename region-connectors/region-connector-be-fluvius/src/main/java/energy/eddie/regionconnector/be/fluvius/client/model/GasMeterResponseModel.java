package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.Granularity;
import jakarta.annotation.Nullable;

import java.util.List;

public record GasMeterResponseModel(
        @JsonProperty("seqNumber") @Nullable Integer seqNumber,
        @JsonProperty("meterID") @Nullable String meterID,
        @JsonProperty("dailyEnergy") @Nullable List<GDailyEnergyItemResponseModel> dailyEnergy,
        @JsonProperty("hourlyEnergy") @Nullable List<GHourlyEnergyItemResponseModel> hourlyEnergy
) implements MeterResponseModel {
    @Nullable
    @Override
    public List<? extends EnergyItemResponseModel<? extends MeasurementResponseModel>> getByGranularity(Granularity granularity) {
        return switch (granularity) {
            case P1D -> dailyEnergy;
            case PT1H -> hourlyEnergy;
            default -> null;
        };
    }
}

