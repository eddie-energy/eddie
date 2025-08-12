package energy.eddie.regionconnector.fi.fingrid.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.Granularity;

import java.time.ZonedDateTime;
import java.util.List;

public record TimeSeries(
        @JsonProperty("MeteringPointEAN") String meteringPointEAN,
        @JsonProperty("ResolutionDuration") Granularity resolutionDuration,
        @JsonProperty("PeriodStartTS") ZonedDateTime start,
        @JsonProperty("PeriodEndTS") ZonedDateTime end,
        @JsonProperty("ProductType") String productType,
        @JsonProperty("UnitType") String unitType,
        @JsonProperty("ReadingType") String readingType,
        @JsonProperty("Observations") List<Observation> observations
) {}
