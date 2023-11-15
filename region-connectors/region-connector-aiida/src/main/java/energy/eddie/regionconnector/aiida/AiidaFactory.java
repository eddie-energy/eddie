package energy.eddie.regionconnector.aiida;

import energy.eddie.regionconnector.aiida.api.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.config.AiidaConfiguration;
import energy.eddie.regionconnector.aiida.dtos.KafkaStreamingConfig;
import energy.eddie.regionconnector.aiida.dtos.PermissionDto;
import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class AiidaFactory {
    private final AiidaConfiguration configuration;

    public AiidaFactory(AiidaConfiguration configuration) {
        this.configuration = configuration;
    }

    public AiidaPermissionRequest createPermissionRequest(
            String connectionId,
            String dataNeedId,
            Instant startTime,
            Instant expirationTime,
            AiidaRegionConnectorService service) {
        var permissionId = UUID.randomUUID().toString();
        return new AiidaPermissionRequest(permissionId, connectionId, dataNeedId, startTime, expirationTime, service);
    }

    public PermissionDto createPermissionDto(AiidaPermissionRequest aiidaRequest) {
        var terminationTopic = terminationTopicForConnectionId(aiidaRequest.connectionId());
        var kafkaConfig = new KafkaStreamingConfig(
                configuration.kafkaBoostrapServers(),
                configuration.kafkaDataTopic(),
                configuration.kafkaStatusMessagesTopic(),
                terminationTopic
        );

        // TODO use dataNeed for service name and requested codes
        return new PermissionDto(
                "My super cool test service",
                aiidaRequest.startTime(),
                aiidaRequest.expirationTime(),
                aiidaRequest.connectionId(),
                Set.of("1-0:1.7.0", "1-0:2.7.0"),
                kafkaConfig
        );
    }

    private String terminationTopicForConnectionId(String connectionId) {
        // TODO no guarantee, that this results in a valid kafka topic name
        return configuration.kafkaTerminationTopicPrefix()
                + "_"
                + connectionId.replace(' ', '_');
    }
}
