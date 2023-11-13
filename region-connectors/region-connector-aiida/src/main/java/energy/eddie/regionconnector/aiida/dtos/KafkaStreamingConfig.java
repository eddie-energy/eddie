package energy.eddie.regionconnector.aiida.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KafkaStreamingConfig(
        @JsonProperty
        String bootstrapServers,
        @JsonProperty
        String dataTopic,
        @JsonProperty
        String statusTopic,
        @JsonProperty
        String subscribeTopic) {
}