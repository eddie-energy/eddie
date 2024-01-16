package energy.eddie.regionconnector.aiida.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.aiida.dtos.TerminationRequest;
import energy.eddie.regionconnector.aiida.permission.request.api.AiidaPermissionRequestInterface;
import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import static energy.eddie.regionconnector.aiida.config.AiidaConfiguration.*;

@Component
public class AiidaKafka {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiidaKafka.class);
    private final ObjectMapper mapper;
    private final AiidaRegionConnectorService service;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Listens for {@link ConnectionStatusMessage}s from any AIIDA instances and will change the states of the
     * associated {@link PermissionRequest} when one is received.
     * <br>
     * Subscribes to the {@code terminationRequestFlux} and will publish a termination request on the specific
     * Kafka termination topic of the associated permission.
     *
     * @param mapper                 ObjectMapper used to deserialize any {@link ConnectionStatusMessage} received via Kafka.
     * @param service                Service used to get permission requests from persistence layer. Needed to be able to update the state of a permission request.
     * @param terminationRequestFlux Flux that contains termination requests that should be processed.
     * @param kafkaTemplate          KafkaTemplate used to send the termination requests to the specific Kafka topics.
     */
    public AiidaKafka(ObjectMapper mapper,
                      AiidaRegionConnectorService service,
                      Flux<TerminationRequest> terminationRequestFlux,
                      KafkaTemplate<String, String> kafkaTemplate
    ) {
        this.mapper = mapper;
        this.service = service;
        this.kafkaTemplate = kafkaTemplate;

        terminationRequestFlux.subscribe(this::sendTerminationRequest);
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
        ConnectionStatusMessage statusMessage = null;
        try {
            statusMessage = mapper.readValue(message, ConnectionStatusMessage.class);

            LOGGER.info("Got new status message: {}", statusMessage);

            AiidaPermissionRequestInterface request = service.getPermissionRequestById(statusMessage.permissionId());

            switch (statusMessage.status()) {
                case ACCEPTED -> request.accept();
                case TERMINATED -> request.terminate();
                case REVOKED -> request.revoke();
                case TIME_LIMIT -> request.timeLimit();
                default -> {
                    if (LOGGER.isErrorEnabled())
                        LOGGER.error("Got status message for permission {} and new status {}, but no handling for the new state is implemented",
                                request.permissionId(), statusMessage.status());
                }
            }
        } catch (JsonProcessingException e) {
            LOGGER.error("Error while deserializing ConnectionStatusMessage", e);
        } catch (PermissionNotFoundException e) {
            LOGGER.error("Got ConnectionStatusMessage {}, but couldn't find a matching permission in the persistence layer.", statusMessage, e);
        } catch (StateTransitionException e) {
            LOGGER.error("Error while transitioning state", e);
        }
    }

    private void sendTerminationRequest(TerminationRequest terminationRequest) {
        LOGGER.info("Sending new terminationRequest {}", terminationRequest);

        var future = kafkaTemplate.send(terminationRequest.terminationTopic(), terminationRequest.connectionId(), terminationRequest.connectionId());
        future.exceptionally(throwable -> {
            LOGGER.error("Error while sending termination request for connectionId {}", terminationRequest.connectionId(), throwable);
            return null;
        });
    }
}
