package energy.eddie.regionconnector.aiida.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Set;

/**
 * This data transfer object contains all necessary information that should be encoded in a QR code, that is then
 * presented to the customer.
 *
 * @param serviceName          Name of the service that the permission is for.
 * @param startTime            UTC start timestamp, from when on data should be shared with the EP.
 * @param expirationTime       UTC timestamp until which data should be shared.
 * @param connectionId         UUID string, that should be sent along with every message related to the requested permission.
 * @param requestedCodes       Set of OBIS codes, that the EP wants to receive from the customer. If the customer's AIIDA system cannot provide <b>all</b> codes, the permission cannot be setup.
 * @param kafkaStreamingConfig Configuration for the streaming via Kafka.
 */
public record PermissionDto(
        @JsonProperty
        String serviceName,
        @JsonProperty
        Instant startTime,
        @JsonProperty
        Instant expirationTime,
        @JsonProperty
        String connectionId,
        @JsonProperty
        Set<String> requestedCodes,
        @JsonProperty
        KafkaStreamingConfig kafkaStreamingConfig) {
}
