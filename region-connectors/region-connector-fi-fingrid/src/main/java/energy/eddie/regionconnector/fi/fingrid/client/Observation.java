package energy.eddie.regionconnector.fi.fingrid.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record Observation(
        @JsonProperty("PeriodStartTime") ZonedDateTime start,
        @JsonProperty("Quantity") BigDecimal quantity,
        @JsonProperty("Quality") String quality
) {}
