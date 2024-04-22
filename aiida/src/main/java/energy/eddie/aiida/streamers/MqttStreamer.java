package energy.eddie.aiida.streamers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.dtos.ConnectionStatusMessage;
import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.aiida.models.record.AiidaRecord;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class MqttStreamer extends AiidaStreamer implements MqttCallback {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttStreamer.class);
    private final MqttStreamingConfig streamingConfig;
    private final MqttAsyncClient client;
    private final ObjectMapper mapper;
    private boolean isBeingTerminated = false;

    /**
     * Creates a new MqttStreamer and initialized the client callback.
     *
     * @param recordFlux             Flux, where records that should be sent are published.
     * @param statusMessageFlux      Flux, where status messages that should be sent are published.
     * @param terminationRequestSink Sink, to which the {@code permissionId} will be published, when the EP requests a
     *                               termination.
     * @param streamingConfig        Necessary MQTT configuration values.
     * @param client                 {@link MqttAsyncClient} used to send to MQTT broker.
     * @param mapper                 {@link ObjectMapper} used to transform the values to be sent into JSON strings.
     */
    protected MqttStreamer(
            Flux<AiidaRecord> recordFlux,
            Flux<ConnectionStatusMessage> statusMessageFlux,
            Sinks.One<String> terminationRequestSink,
            MqttStreamingConfig streamingConfig,
            MqttAsyncClient client,
            ObjectMapper mapper
    ) {
        super(recordFlux, statusMessageFlux, terminationRequestSink);
        this.streamingConfig = streamingConfig;
        this.mapper = mapper;
        this.client = client;

        client.setCallback(this);
    }

    @Override
    public void connect() {
        MqttConnectionOptions connOpts = new MqttConnectionOptions();
        connOpts.setCleanStart(false);
        connOpts.setAutomaticReconnect(true);
        connOpts.setAutomaticReconnectDelay(30, 60 * 5);
        connOpts.setUserName(streamingConfig.username());
        connOpts.setPassword(streamingConfig.password().getBytes(StandardCharsets.UTF_8));

        try {
            LOGGER.info("MqttStreamer for permission {} connecting to broker {} with username {}",
                        streamingConfig.permissionId(),
                        client.getServerURI(),
                        streamingConfig.username());
            client.connect(connOpts);
        } catch (MqttException exception) {
            LOGGER.error("MqttStreamer for permission {} has encountered an error while connecting to MQTT broker",
                         streamingConfig.permissionId(),
                         exception);
            return;
        }

        recordFlux.publishOn(Schedulers.boundedElastic()).subscribe(this::publishRecord);
        statusMessageFlux.publishOn(Schedulers.boundedElastic()).subscribe(this::publishStatusMessage);
    }

    private void publishRecord(AiidaRecord aiidaRecord) {
        if (isBeingTerminated) {
            LOGGER.debug(
                    "MqttStreamer for permission {} got AiidaRecord {} to publish but the streamer has already received a termination request, and will therefore not send the AiidaRecord",
                    streamingConfig.permissionId(),
                    aiidaRecord);
            return;
        }

        LOGGER.trace("MqttStreamer for permission {} publishing AiidaRecord {}",
                     streamingConfig.permissionId(),
                     aiidaRecord);

        try {
            byte[] jsonBytes = mapper.writeValueAsBytes(aiidaRecord);
            client.publish(streamingConfig.dataTopic(), jsonBytes, 1, false);
        } catch (JsonProcessingException | MqttException exception) {
            LOGGER.error(
                    "MqttStreamer for permission {} has encountered an error while converting and sending AiidaRecord {}",
                    streamingConfig.permissionId(),
                    aiidaRecord,
                    exception);
        }
    }

    private void publishStatusMessage(ConnectionStatusMessage connectionStatusMessage) {
        if (isBeingTerminated) {
            LOGGER.debug(
                    "MqttStreamer for permission {} got ConnectionStatusMessage {} to publish but the streamer has already received a termination request, and will therefore not send the ConnectionStatusMessage",
                    streamingConfig.permissionId(),
                    connectionStatusMessage);
            return;
        }

        LOGGER.trace("MqttStreamer for permission {} publishing connectionStatusMessage {}",
                     streamingConfig.permissionId(),
                     connectionStatusMessage);

        try {
            byte[] jsonBytes = mapper.writeValueAsBytes(connectionStatusMessage);
            client.publish(streamingConfig.statusTopic(), jsonBytes, 1, false);
        } catch (JsonProcessingException | MqttException exception) {
            LOGGER.error(
                    "MqttStreamer for permission {} has encountered an error while converting and sending ConnectionStatusMessage {}",
                    streamingConfig.permissionId(),
                    connectionStatusMessage,
                    exception);
        }
    }

    @Override
    public void close() {
        terminationRequestSink.tryEmitEmpty();

        LOGGER.info("Closing MqttStreamer for permission {}", streamingConfig.permissionId());
        try {
            client.disconnect(2000).waitForCompletion();
            client.close();
        } catch (MqttException e) {
            LOGGER.info("MqttStreamer for permission {} has encountered an error while disconnecting",
                        streamingConfig.permissionId());
        }
    }

    @Override
    public void disconnected(MqttDisconnectResponse disconnectResponse) {
        LOGGER.info("MqttStreamer for permission {} has disconnected from remote server",
                    streamingConfig.permissionId(),
                    disconnectResponse.getException());
    }

    @Override
    public void mqttErrorOccurred(MqttException exception) {
        LOGGER.error("Error in MqttStreamer for permission {}", streamingConfig.permissionId(), exception);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        LOGGER.info("Got termination message {}", message);

        if (!payload.equals(streamingConfig.permissionId())) {
            LOGGER.warn(
                    "MqttStreamer got request to terminate permission {}, but received wrong permission.permissionId() {}",
                    streamingConfig.permissionId(),
                    payload);
            return;
        }

        isBeingTerminated = true;
        terminationRequestSink.emitValue(payload, Sinks.EmitFailureHandler.busyLooping(Duration.ofMinutes(3)));
    }

    @Override
    public void deliveryComplete(IMqttToken token) {
        LOGGER.trace("MqttStreamer for permission {}, delivery complete {}", streamingConfig.permissionId(), token);
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        LOGGER.debug("MqttStreamer for permission {} has connected to server {}, was because of reconnect: {}",
                     streamingConfig.permissionId(),
                     serverURI,
                     reconnect);

        try {
            client.subscribe(streamingConfig.terminationTopic(), 2);
        } catch (MqttException e) {
            LOGGER.error("MqttStreamer for permission {} has encountered an error while subscribing to {} ",
                         streamingConfig.permissionId(),
                         streamingConfig.terminationTopic(),
                         e);
        }
    }

    @Override
    public void authPacketArrived(int reasonCode, MqttProperties properties) {
        // Not needed
    }
}
