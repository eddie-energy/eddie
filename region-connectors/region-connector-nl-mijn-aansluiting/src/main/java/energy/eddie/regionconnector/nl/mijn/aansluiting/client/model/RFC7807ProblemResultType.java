package energy.eddie.regionconnector.nl.mijn.aansluiting.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RFC7807ProblemResultType(
        @JsonProperty("title") String title,
        @JsonProperty("type") String type,
        @JsonProperty("detail") String detail,
        @JsonProperty("status") Integer status,
        @JsonProperty("instance") String instance
) {}

