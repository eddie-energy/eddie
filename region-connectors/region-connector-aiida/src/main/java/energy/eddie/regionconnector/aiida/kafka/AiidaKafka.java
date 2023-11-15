package energy.eddie.regionconnector.aiida.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.aiida.api.AiidaPermissionRequestRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static energy.eddie.regionconnector.aiida.config.AiidaConfiguration.*;

@Component
public class AiidaKafka {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiidaKafka.class);
    private final ObjectMapper mapper;
    private final AiidaPermissionRequestRepository repository;

    public AiidaKafka(ObjectMapper mapper, AiidaPermissionRequestRepository repository) {
        this.mapper = mapper;
        this.repository = repository;
    }

    /**
     * Listens for {@link ConnectionStatusMessage}s from AIIDA instances and updates the states of the
     * associated {@link PermissionRequest}s accordingly.
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

            LOGGER.info("new status message {}", statusMessage);

            var optionalRequest = repository.findByConnectionId(statusMessage.connectionId());

            if (optionalRequest.isEmpty()) {
                LOGGER.warn("Got ConnectionStatusMessage {}, but couldn't find a matching permission in the database.", statusMessage);
                return;
            }

            var request = optionalRequest.get();

            switch (statusMessage.status()) {
                // TODO should check if permission hasn't expired yet?
                // TODO what exactly should be done for TIME_LIMIT and TERMINATED messages?
                case ACCEPTED -> request.accept();
                case TERMINATED -> request.terminate();
                case REVOKED -> request.revoke();
                case TIME_LIMIT -> request.timeLimit();
                default ->
                        LOGGER.error("Got status message for permission {} and new status {}, but no handling for the new state is implemented",
                                request.permissionId(), statusMessage.status());
            }

            repository.save(request);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error while deserializing ConnectionStatusMessage", e);
        } catch (StateTransitionException e) {
            LOGGER.error("Error while transitioning state", e);
        }
    }
}
