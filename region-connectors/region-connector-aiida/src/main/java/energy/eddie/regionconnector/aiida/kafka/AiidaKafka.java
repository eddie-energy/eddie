package energy.eddie.regionconnector.aiida.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionRequestViewRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static energy.eddie.regionconnector.aiida.config.AiidaConfiguration.*;

@Component
public class AiidaKafka {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiidaKafka.class);
    private final ObjectMapper mapper;
    private final Outbox outbox;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AiidaPermissionRequestViewRepository repository;

    /**
     * Listens for {@link ConnectionStatusMessage}s from any AIIDA instances and will change the states of the
     * associated {@link PermissionRequest} when one is received.
     * <br>
     * Subscribes to the {@code terminationRequestFlux} and will publish a termination request on the specific Kafka
     * termination topic of the associated permission.
     *
     * @param mapper        ObjectMapper used to deserialize any {@link ConnectionStatusMessage} received via Kafka.
     * @param kafkaTemplate KafkaTemplate used to send the termination requests to the specific Kafka topics.
     * @param outbox        Outbox used to send {@link PermissionEvent}s
     */
    public AiidaKafka(
            ObjectMapper mapper,
            KafkaTemplate<String, String> kafkaTemplate,
            Outbox outbox,
            AiidaPermissionRequestViewRepository repository
    ) {
        this.mapper = mapper;
        this.outbox = outbox;
        this.kafkaTemplate = kafkaTemplate;
        this.repository = repository;
    }

    /**
     * Listens for {@link ConnectionStatusMessage}s from AIIDA instances and updates the states of the associated
     * {@link PermissionRequest}s accordingly.
     *
     * @param message Message as received from the Kafka broker.
     */
    @KafkaListener(id = "region-connector-aiida", topics = "${" + KAFKA_STATUS_MESSAGES_TOPIC + "}",
            groupId = "${" + KAFKA_GROUP_ID + "}",
            properties = {
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG + "=${" + KAFKA_BOOTSTRAP_SERVERS + "}",
                    // don't miss any messages
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG + "=earliest"
            })
    public void listenForConnectionStatusMessages(String message) {
        try {
            var statusMessage = mapper.readValue(message, ConnectionStatusMessage.class);

            LOGGER.info("Got new status message: {}", statusMessage);

            String permissionId = statusMessage.permissionId();

            switch (statusMessage.status()) {
                case ACCEPTED -> outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.ACCEPTED));
                case TERMINATED -> outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.TERMINATED));
                case REVOKED -> outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.REVOKED));
                case FULFILLED -> outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.FULFILLED));
                default -> {
                    if (LOGGER.isErrorEnabled())
                        LOGGER.error(
                                "Got status message for permission {} and new status {}, but no handling for the new status is implemented",
                                permissionId,
                                statusMessage.status());
                }
            }
        } catch (JsonProcessingException e) {
            LOGGER.error("Error while deserializing ConnectionStatusMessage", e);
        }
    }

    public void sendTerminationRequest(String permissionId) {
        Optional<AiidaPermissionRequest> optionalPermissionRequest = repository.findById(permissionId);

        if (optionalPermissionRequest.isEmpty()) {
            LOGGER.atError()
                  .addArgument(permissionId)
                  .log("Was requested to terminate permission {}, but could not find a matching permission in the repository");
            return;
        }

        var permissionRequest = optionalPermissionRequest.get();

        try {
            var future = kafkaTemplate.send(permissionRequest.terminationTopic(),
                                            permissionRequest.connectionId(),
                                            permissionRequest.connectionId());
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    LOGGER.error("Error while sending termination request for permission {}",
                                 permissionRequest.permissionId(),
                                 ex);
                    return;
                }
                outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.TERMINATED));
            });
        } catch (RuntimeException ex) {
            LOGGER.error("Kafka throw a runtime exception while sending termination request", ex);
        }
    }
}
