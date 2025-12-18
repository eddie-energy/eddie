package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;
import java.util.List;

public record GDailyEnergyItemResponseModel(
        @JsonProperty("timestampStart") @Nullable ZonedDateTime timestampStart,
        @JsonProperty("timestampEnd") @Nullable ZonedDateTime timestampEnd,
        @JsonProperty("measurement") @Nullable List<GMeasurementItemResponseModel> measurement
) {
}

