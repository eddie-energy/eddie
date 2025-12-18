package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;
import java.util.List;

public record EDailyEnergyItemResponseModel(
        @Nullable @JsonProperty("timestampStart") ZonedDateTime timestampStart,
        @Nullable @JsonProperty("timestampEnd") ZonedDateTime timestampEnd,
        @JsonProperty("measurement") @Nullable List<EMeasurementItemResponseModel> measurement
) {}

