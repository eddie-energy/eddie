package energy.eddie.aiida.models.permission;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "kafka_streaming_config")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KafkaStreamingConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Nullable
    @Column(name = "id")
    // id field is required by JPA but not used in business logic
    @SuppressWarnings("unused")
    private Long id;
    @Schema(description = "Comma separated list of Kafka boostrap servers.", example = "localhost:9092")
    @Column(nullable = false)
    @NotBlank(message = "bootstrapServers must not be null or blank.")
    @JsonProperty(required = true)
    private String bootstrapServers;
    @Schema(description = "Kafka topic to which the near real-time data should be sent.", example = "SomeDataTopic")
    @Column(nullable = false)
    @NotBlank(message = "dataTopic must not be null or blank.")
    @JsonProperty(required = true)
    private String dataTopic;
    @Schema(description = "Kafka topic to which the connection status messages should be sent.", example = "SomeStatusTopic")
    @Column(nullable = false)
    @NotBlank(message = "statusTopic must not be null or blank.")
    @JsonProperty(required = true)
    private String statusTopic;
    @Schema(description = "Kafka topic on which the EP/EDDIE publishers termination requests and AIIDA subscribes to.", example = "SomeSubscribeTopic")
    @Column(nullable = false)
    @NotBlank(message = "subscribeTopic must not be null or blank.")
    @JsonProperty(required = true)
    private String subscribeTopic;

    /**
     * Creates a new KafkaConfig with the specified configuration.
     *
     * @param bootstrapServers Comma separated list of Kafka servers to connect to.
     * @param dataTopic        Topic to which AIIDA should publish the data messages.
     * @param statusTopic      Topic to which AIIDA should publish messages resulting from the permission process model (e.g. permission accepted).
     * @param subscribeTopic   Topic on which AIIDA should subscribe to, where the framework publishes EP termination requests.
     */
    public KafkaStreamingConfig(String bootstrapServers, String dataTopic, String statusTopic, String subscribeTopic) {
        this.bootstrapServers = bootstrapServers;
        this.dataTopic = dataTopic;
        this.statusTopic = statusTopic;
        this.subscribeTopic = subscribeTopic;
    }

    @SuppressWarnings("NullAway.Init")
    protected KafkaStreamingConfig() {
    }

    /**
     * Returns the comma separated list of servers that the Kafka client should connect to.
     */
    public String bootstrapServers() {
        return bootstrapServers;
    }

    /**
     * Returns the topic to which AIIDA should publish the data messages.
     */
    public String dataTopic() {
        return dataTopic;
    }

    /**
     * Returns the topic to which AIIDA should publish messages resulting from the permission process model (e.g. permission accepted).
     */
    public String statusTopic() {
        return statusTopic;
    }

    /**
     * Returns the topic on which AIIDA should subscribe to, where the framework publishes EP termination requests.
     */
    public String subscribeTopic() {
        return subscribeTopic;
    }
}