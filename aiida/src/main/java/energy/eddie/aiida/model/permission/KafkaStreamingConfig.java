package energy.eddie.aiida.model.permission;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import static java.util.Objects.requireNonNull;

@Entity
public class KafkaStreamingConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Nullable
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "bootstrapServers mustn't be null or blank.")
    private String bootstrapServers;
    @Column(nullable = false)
    @NotBlank(message = "publishTopic mustn't be null or blank.")
    private String publishTopic;
    @Column(nullable = false)
    @NotBlank(message = "subscribeTopic mustn't be null or blank.")
    private String subscribeTopic;

    /**
     * Creates a new KafkaConfig with the specified configuration.
     *
     * @param bootstrapServers Comma separated list of Kafka servers to connect to.
     * @param publishTopic     Topic to which AIIDA should publish the data messages.
     * @param subscribeTopic   Topic on which AIIDA should subscribe to, where the framework publishes EP termination requests.
     */
    public KafkaStreamingConfig(String bootstrapServers, String publishTopic, String subscribeTopic) {
        this.bootstrapServers = requireNonNull(bootstrapServers);
        this.publishTopic = requireNonNull(publishTopic);
        this.subscribeTopic = requireNonNull(subscribeTopic);
    }

    @SuppressWarnings("NullAway.Init")
    protected KafkaStreamingConfig() {
    }

    public String bootstrapServers() {
        return bootstrapServers;
    }

    public String publishTopic() {
        return publishTopic;
    }

    public String subscribeTopic() {
        return subscribeTopic;
    }
}