package energy.eddie.regionconnector.aiida.services;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.aiida.config.AiidaConfiguration;
import energy.eddie.regionconnector.aiida.dtos.KafkaStreamingConfig;
import energy.eddie.regionconnector.aiida.dtos.PermissionDto;
import energy.eddie.regionconnector.aiida.dtos.PermissionRequestForCreation;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.util.Set;
import java.util.UUID;

@Service
public class AiidaRegionConnectorService implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiidaRegionConnectorService.class);
    private final AiidaConfiguration configuration;
    private final Sinks.Many<ConnectionStatusMessage> statusMessageSink = Sinks.many().multicast().onBackpressureBuffer();

    @Autowired
    public AiidaRegionConnectorService(AiidaConfiguration configuration) {
        this.configuration = configuration;
    }

    public Publisher<ConnectionStatusMessage> connectionStatusMessageFlux() {
        return statusMessageSink.asFlux();
    }

    @Override
    public void close() {
        statusMessageSink.tryEmitComplete();
    }

    public PermissionDto createNewPermission(PermissionRequestForCreation request) {
        var kafkaConfig = new KafkaStreamingConfig(
                configuration.kafkaBoostrapServers(),
                configuration.kafkaDataTopic(),
                configuration.kafkaStatusMessagesTopic(),
                configuration.kafkaTerminationTopicPrefix() + "_" + request.connectionId().replace(' ', '_')
        );

        var dto = new PermissionDto(
                "My super cool test service",
                request.startTime(),
                request.expirationTime(),
                request.connectionId(),
                Set.of("1-0:1.8.0", "1-0:2.8.0"),
                kafkaConfig
        );

        var statusMessage = new ConnectionStatusMessage(request.connectionId(), UUID.randomUUID().toString(),
                request.dataNeedId(), PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
        var result = statusMessageSink.tryEmitNext(statusMessage);

        if (result.isFailure())
            LOGGER.error("Error while emitting ConnectionStatusMessage for new permission with connectionId {}. Error was {}", request.connectionId(), result);

        return dto;
    }
}
