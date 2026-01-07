package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.List;

public record GHourlyEnergyItemResponseModel(
        @JsonProperty("timestampStart") ZonedDateTime timestampStart,
        @JsonProperty("timestampEnd") ZonedDateTime timestampEnd,
        @JsonProperty("measurement") List<GMeasurementItemResponseModel> measurement
) implements EnergyItemResponseModel<GMeasurementItemResponseModel> {}

