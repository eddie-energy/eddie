package energy.eddie.aiida.streamers;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.repositories.FailedToSendRepository;
import energy.eddie.aiida.utils.MqttFactory;
import org.eclipse.paho.mqttv5.client.persist.MqttDefaultFilePersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import static java.util.Objects.requireNonNull;

public class StreamerFactory {
    private StreamerFactory() {
    }

    /**
     * Creates a new {@link AiidaStreamer} applying the specified streamingConfig.
     *
     * @param permission             Permission for which to create the AiidaStreamer.
     * @param recordFlux             Flux on which the records that should be sent are published.
     * @param terminationRequestSink Sink, to which the permissionId will be published, when the EP requests a
     *                               termination.
     * @param mapper                 {@link ObjectMapper} that should be used to convert the records to JSON.
     * @param failedToSendRepository Repository to save messages that could not be sent.
     * @throws MqttException If the creation of the MqttClient failed.
     */
    protected static AiidaStreamer getAiidaStreamer(
            Permission permission,
            Flux<AiidaRecord> recordFlux,
            Sinks.One<String> terminationRequestSink,
            ObjectMapper mapper,
            FailedToSendRepository failedToSendRepository
    ) throws MqttException {
        var mqttConfig = requireNonNull(permission.mqttStreamingConfig());
        String permissionId = permission.permissionId();

        var client = MqttFactory.getMqttAsyncClient(mqttConfig.serverUri(),
                                                    mqttConfig.username(),
                                                    new MqttDefaultFilePersistence("mqtt-persistence/" + permissionId));
        return new MqttStreamer(recordFlux, terminationRequestSink, mqttConfig, client, mapper, failedToSendRepository);
    }
}
