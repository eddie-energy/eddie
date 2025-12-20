package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;
import java.util.List;

public record GetEnergyResponseModel(
        @JsonProperty("fetchTime") @Nullable ZonedDateTime fetchTime,
        @JsonProperty("gasMeters") @Nullable List<GasMeterResponseModel> gasMeters,
        @JsonProperty("electricityMeters") @Nullable List<ElectricityMeterResponseModel> electricityMeters
) {
    public List<MeterResponseModel> getMeterFor(EnergyType energyType) {
        return switch (energyType) {
            case ELECTRICITY -> copyOrEmpty(electricityMeters);
            case NATURAL_GAS -> copyOrEmpty(gasMeters);
            default -> List.of();
        };
    }

    private static List<MeterResponseModel> copyOrEmpty(@Nullable List<? extends MeterResponseModel> source) {
        return source == null ? List.of() : List.copyOf(source);
    }
}

