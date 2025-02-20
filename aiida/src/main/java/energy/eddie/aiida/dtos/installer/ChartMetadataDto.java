package energy.eddie.aiida.dtos.installer;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChartMetadataDto(
        @JsonProperty("name") String name,
        @JsonProperty("version") String version,
        @JsonProperty("description") String description,
        @JsonProperty("appVersion") String appVersion,
        @JsonProperty("deprecated") boolean deprecated
) { }