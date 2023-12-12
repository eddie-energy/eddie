package energy.eddie.regionconnector.aiida;

import energy.eddie.regionconnector.aiida.api.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.config.AiidaConfiguration;
import energy.eddie.regionconnector.aiida.dtos.KafkaStreamingConfig;
import energy.eddie.regionconnector.aiida.dtos.PermissionDto;
import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.internals.Topic;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Component
public class AiidaFactory {
    private final AiidaConfiguration configuration;

    public AiidaFactory(AiidaConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Creates and populates a new permission request.
     * TODO: The dataNeed API is queried for the required information (e.g. start, expiration time).
     *
     * @param connectionId connectionId that should be used for this new permission request.
     * @param dataNeedId   dataNeedId that should be used for this new permission request.
     * @param service      Reference to the service that allows the request to transition states.
     * @return Populated permission request.
     */
    public AiidaPermissionRequest createPermissionRequest(
            String connectionId,
            String dataNeedId,
            AiidaRegionConnectorService service) {
        // TODO get start and expiration from dataNeed API --> follow-up issue: #431
        var startTime = Instant.now();
        var expirationTime = startTime.plusSeconds(864000); // roughly 10 days

        var permissionId = UUID.randomUUID().toString();
        var terminationTopic = terminationTopicForPermissionId(permissionId);
        return new AiidaPermissionRequest(permissionId, connectionId, dataNeedId, terminationTopic, startTime, expirationTime, service);
    }

    /**
     * Creates and populates a new {@link PermissionDto} with values from configuration.
     *
     * @param aiidaRequest Request for which a PermissionDto should be created.
     * @return Populated PermissionDto.
     * @throws InvalidTopicException If topic name for the termination topic is invalid. This indicates that either the
     *                               permissionId is not a valid UUID-4, or the prefix from the configuration is invalid.
     */
    public PermissionDto createPermissionDto(AiidaPermissionRequest aiidaRequest) throws InvalidTopicException {
        var kafkaConfig = new KafkaStreamingConfig(
                configuration.kafkaBoostrapServers(),
                configuration.kafkaDataTopic(),
                configuration.kafkaStatusMessagesTopic(),
                aiidaRequest.terminationTopic()
        );

        // TODO use dataNeed for service name and requested codes
        return new PermissionDto(
                UUID.randomUUID().toString(),
                "My super cool test service",
                aiidaRequest.dataNeedId(),
                aiidaRequest.startTime(),
                aiidaRequest.expirationTime(),
                aiidaRequest.connectionId(),
                Set.of("1-0:1.7.0", "1-0:2.7.0"),
                kafkaConfig
        );
    }

    /**
     * Creates the termination topic name by concatenating the termination topic prefix and the permissionId with an underscore.
     *
     * @param permissionId Id of permission request
     * @return Kafka topic name
     * @throws InvalidTopicException If the resulting topic name is invalid. This indicates that either
     *                               the permissionId is not a valid UUID-4, or the prefix is invalid.
     */
    private String terminationTopicForPermissionId(String permissionId) throws InvalidTopicException {
        var topic = configuration.kafkaTerminationTopicPrefix() + "_" + permissionId;
        Topic.validate(topic);
        return topic;
    }
}
