package energy.eddie.regionconnector.nl.mijn.aansluiting.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

public record DateAndOrTime(@JsonProperty("DateTime") ZonedDateTime dateTime) {}

