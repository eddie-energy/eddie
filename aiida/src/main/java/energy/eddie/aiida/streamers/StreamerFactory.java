package energy.eddie.aiida.streamers;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.dtos.ConnectionStatusMessage;
import energy.eddie.aiida.errors.StreamerCreationFailedException;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.utils.MqttFactory;
import org.eclipse.paho.mqttv5.client.persist.MqttDefaultFilePersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public class StreamerFactory {
    private StreamerFactory() {
    }

    /**
     * Creates a new {@link AiidaStreamer} applying the specified streamingConfig.
     *
     * @param permission             Permission for which to create the AiidaStreamer.
     * @param recordFlux             Flux on which the records that should be sent are published.
     * @param statusMessageFlux      Flux on which status messages that should be sent are published.
     * @param terminationRequestSink Sink, to which the permissionId will be published, when the EP requests a
     *                               termination.
     * @param mapper                 {@link ObjectMapper} that should be used to convert the records to JSON.
     */
    protected static AiidaStreamer getAiidaStreamer(
            Permission permission,
            Flux<AiidaRecord> recordFlux,
            Flux<ConnectionStatusMessage> statusMessageFlux,
            Sinks.One<String> terminationRequestSink,
            ObjectMapper mapper
    ) {
        var mqttConfig = permission.mqttStreamingConfig();

        String permissionId = permission.permissionId();
        if (mqttConfig == null) {
            // TODO unwrap once PermissionService is reworked --> GH-929
            throw new RuntimeException(new StreamerCreationFailedException(
                    "MqttStreamingConfig for permission '%s' is null".formatted(
                            permissionId)));
        }

        try {
            var client = MqttFactory.getMqttAsyncClient(mqttConfig.serverUri(),
                                                        mqttConfig.username(),
                                                        new MqttDefaultFilePersistence("mqtt-persistence/" + permissionId));
            return new MqttStreamer(recordFlux,
                                    statusMessageFlux,
                                    terminationRequestSink,
                                    mqttConfig,
                                    client,
                                    mapper);
        } catch (MqttException exception) {
            // TODO unwrap once PermissionService is reworked --> GH-929
            throw new RuntimeException(new StreamerCreationFailedException(exception));
        }
    }
}
