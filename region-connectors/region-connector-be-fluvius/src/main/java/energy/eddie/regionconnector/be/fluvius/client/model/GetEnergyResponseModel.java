package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;
import java.util.List;

public record GetEnergyResponseModel(
        @JsonProperty("fetchTime") @Nullable ZonedDateTime fetchTime,
        @JsonProperty("gasMeters") @Nullable List<GasMeterResponseModel> gasMeters,
        @JsonProperty("electricityMeters") @Nullable List<ElectricityMeterResponseModel> electricityMeters
) {}

