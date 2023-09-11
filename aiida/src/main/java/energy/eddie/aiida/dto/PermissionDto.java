package energy.eddie.aiida.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.constraints.ExpirationTimeAfterStartTime;
import energy.eddie.aiida.model.permission.KafkaStreamingConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Set;

@ExpirationTimeAfterStartTime
public record PermissionDto(
        @NotBlank(message = "serviceName mustn't be null or blank.")
        @JsonProperty(required = true)
        String serviceName,
        @NotNull(message = "startTime mustn't be null.")
        @JsonProperty(required = true)
        Instant startTime,
        @NotNull(message = "expirationTime mustn't be null.")
        @JsonProperty(required = true)
        Instant expirationTime,
        @NotNull(message = "grantTime mustn't be null.")
        @JsonProperty(required = true)
        Instant grantTime,
        @NotBlank(message = "connectionId mustn't be null or blank.")
        @JsonProperty(required = true)
        String connectionId,
        @NotEmpty(message = "At least one OBIS code needs to be requested.")
        @JsonProperty(required = true)
        Set<String> requestedCodes,
        @Valid  // if PermissionDto is validated, also validate kafkaStreamingConfig
        @JsonProperty(required = true)
        @NotNull(message = "kafkaStreamingConfig mustn't be null.")
        KafkaStreamingConfig kafkaStreamingConfig) {
}
