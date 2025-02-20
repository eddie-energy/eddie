package energy.eddie.aiida.dtos.installer;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

public record ReleaseInfoDto(
        @JsonProperty("firstDeployed") ZonedDateTime firstDeployed,
        @JsonProperty("lastDeployed") ZonedDateTime lastDeployed,
        @JsonProperty("deleted") ZonedDateTime deleted,
        @JsonProperty("description") String description,
        @JsonProperty("status") String status
) { }