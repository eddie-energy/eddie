package energy.eddie.regionconnector.nl.mijn.aansluiting.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record Reading(
        @JsonProperty("DateAndOrTime")
        DateAndOrTime dateAndOrTime,
        @JsonProperty("ReadingType")
        ReadingType readingType,
        @JsonProperty("Value")
        BigDecimal value
) {}

