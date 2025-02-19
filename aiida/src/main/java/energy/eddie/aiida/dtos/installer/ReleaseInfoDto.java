package energy.eddie.aiida.dtos.installer;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

public record ReleaseInfoDto(
        @JsonProperty("first_deployed") ZonedDateTime firstDeployed,
        @JsonProperty("last_deployed") ZonedDateTime lastDeployed,
        @JsonProperty("deleted") ZonedDateTime deleted,
        @JsonProperty("description") String description,
        @JsonProperty("status") String status,
        @JsonProperty("notes") String notes
) { }