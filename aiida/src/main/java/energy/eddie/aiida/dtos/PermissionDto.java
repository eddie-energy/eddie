package energy.eddie.aiida.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.constraints.ExpirationTimeAfterStartTime;
import energy.eddie.aiida.models.permission.KafkaStreamingConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Set;

/**
 * This data transfer object is expected by the permission API endpoint to set up a new permission.
 * All parameters are required.
 *
 * @param serviceName          Name of the service that the permission is for.
 * @param startTime            UTC start timestamp, from when on data should be shared with the EP.
 * @param expirationTime       UTC timestamp until which data should be shared.
 * @param grantTime            UTC timestamp when the customer granted the permission.
 * @param connectionId         UUID string, that should be sent along with every message related to the requested permission.
 * @param requestedCodes       Set of OBIS codes, that the EP wants to receive from the customer. If the customer's AIIDA system cannot provide <b>all</b> codes, the permission cannot be setup.
 * @param kafkaStreamingConfig Configuration for the streaming via Kafka.
 */
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
