package energy.eddie.aiida.model.permission;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import static java.util.Objects.requireNonNull;

@Entity
@Table(name = "kafka_streaming_config")
public class KafkaStreamingConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Nullable
    @Column(name = "id")
    private Long id;
    @Column(nullable = false)
    @NotBlank(message = "bootstrapServers mustn't be null or blank.")
    private String bootstrapServers;
    @Column(nullable = false)
    @NotBlank(message = "dataTopic mustn't be null or blank.")
    private String dataTopic;
    @Column(nullable = false)
    @NotBlank(message = "statusTopic mustn't be null or blank.")
    private String statusTopic;
    @Column(nullable = false)
    @NotBlank(message = "subscribeTopic mustn't be null or blank.")
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
        this.bootstrapServers = requireNonNull(bootstrapServers);
        this.dataTopic = requireNonNull(dataTopic);
        this.statusTopic = requireNonNull(statusTopic);
        this.subscribeTopic = requireNonNull(subscribeTopic);
    }

    @SuppressWarnings("NullAway.Init")
    protected KafkaStreamingConfig() {
    }

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