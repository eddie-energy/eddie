package energy.eddie.regionconnector.nl.mijn.aansluiting.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RFC7807WithErrorCodeProblemResultType(
        @JsonProperty("title") String title,
        @JsonProperty("type") String type,
        @JsonProperty("detail") String detail,
        @JsonProperty("status") Integer status,
        @JsonProperty("errorCode") String errorCode,
        @JsonProperty("instance") String instance
) {}