package energy.eddie.aiida.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DataSourceTypeDto(
        @JsonProperty("identifier") String identifier,
        @JsonProperty("name") String name) { }