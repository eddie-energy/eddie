package energy.eddie.aiida.dtos.installer;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChartMetadataDto(
        @JsonProperty("name") String name,
        @JsonProperty("version") String version,
        @JsonProperty("description") String description,
        @JsonProperty("apiVersion") String apiVersion,
        @JsonProperty("appVersion") String appVersion,
        @JsonProperty("type") String type,
        @JsonProperty("deprecated") boolean deprecated
) { }