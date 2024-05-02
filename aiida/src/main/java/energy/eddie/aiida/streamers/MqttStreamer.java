package energy.eddie.aiida.streamers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.dtos.ConnectionStatusMessage;
import energy.eddie.aiida.models.FailedToSendEntity;
import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.repositories.FailedToSendRepository;
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
import java.util.List;

public class MqttStreamer extends AiidaStreamer implements MqttCallback {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttStreamer.class);
    private final MqttStreamingConfig streamingConfig;
    private final MqttAsyncClient client;
    private final ObjectMapper mapper;
    private final FailedToSendRepository failedToSendRepository;
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
            ObjectMapper mapper,
            FailedToSendRepository failedToSendRepository
    ) {
        super(recordFlux, statusMessageFlux, terminationRequestSink);
        this.streamingConfig = streamingConfig;
        this.mapper = mapper;
        this.client = client;
        this.failedToSendRepository = failedToSendRepository;

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

        // pending tokens are messages that are saved by the MqttClient to its persistence config
        LOGGER.atInfo()
              .addArgument(streamingConfig.permissionId())
              .addArgument(client.getPendingTokens().length)
              .log("MqttStreamer for permission {} has {} pending tokens that will be automatically delivered once it connects");
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
        LOGGER.trace("MqttStreamer for permission {} publishing AiidaRecord {}",
                     streamingConfig.permissionId(),
                     aiidaRecord);

        try {
            byte[] jsonBytes = mapper.writeValueAsBytes(aiidaRecord);
            publishMessage(streamingConfig.dataTopic(), jsonBytes);
        } catch (JsonProcessingException exception) {
            LOGGER.error("MqttStreamer for permission {} cannot convert AiidaRecord {} to JSON, will ignore it",
                         streamingConfig.permissionId(),
                         aiidaRecord,
                         exception);
        }
    }

    private void publishStatusMessage(ConnectionStatusMessage connectionStatusMessage) {
        LOGGER.trace("MqttStreamer for permission {} publishing connectionStatusMessage {}",
                     streamingConfig.permissionId(),
                     connectionStatusMessage);

        try {
            byte[] jsonBytes = mapper.writeValueAsBytes(connectionStatusMessage);
            publishMessage(streamingConfig.statusTopic(), jsonBytes);
        } catch (JsonProcessingException exception) {
            LOGGER.error(
                    "MqttStreamer for permission {} cannot convert ConnectionStatusMessage {} to JSON, will ignore it",
                    streamingConfig.permissionId(),
                    connectionStatusMessage,
                    exception);
        }
    }

    private void publishMessage(String topic, byte[] payload) {
        if (isBeingTerminated) {
            LOGGER.atDebug()
                  .addArgument(streamingConfig.permissionId())
                  .addArgument(new String(payload, StandardCharsets.UTF_8))
                  .addArgument(topic)
                  .log("MqttStreamer for permission {} got message {} to publish to topic {} but the streamer has already received a termination request, and will therefore not send the message");
            return;
        }

        try {
            // if client is not connected, it will not save published messages to its persistence module, but instead
            // throws an exception, therefore we need to manually save messages that failed to send
            client.publish(topic, payload, 1, false);
        } catch (MqttException exception) {
            LOGGER.atTrace()
                  .addArgument(streamingConfig.permissionId())
                  .addArgument(new String(payload, StandardCharsets.UTF_8))
                  .addArgument(topic)
                  .log("MqttStreamer for permission {} failed to send message {} to topic {}, will store it in the DB",
                       exception);
            // TODO need to delete these messages again if permission expires --> GH-981
            failedToSendRepository.save(new FailedToSendEntity(streamingConfig.permissionId(), topic, payload));
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
            LOGGER.info("MqttStreamer for permission {} has encountered an error while disconnecting/closing streamer",
                        streamingConfig.permissionId(), e);
        }
    }

    @Override
    public void disconnected(MqttDisconnectResponse disconnectResponse) {
        LOGGER.warn("MqttStreamer for permission {} has disconnected from remote server {}",
                    streamingConfig.permissionId(),
                    disconnectResponse.getServerReference(),
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

        subscribeToTerminationTopic();
        retryFailedToSendMessages();
    }

    private void subscribeToTerminationTopic() {
        try {
            client.subscribe(streamingConfig.terminationTopic(), 2);
        } catch (MqttException e) {
            LOGGER.error("MqttStreamer for permission {} has encountered an error while subscribing to {} ",
                         streamingConfig.permissionId(),
                         streamingConfig.terminationTopic(),
                         e);
        }
    }

    private void retryFailedToSendMessages() {
        List<FailedToSendEntity> failedToSend = failedToSendRepository.findAllByPermissionId(streamingConfig.permissionId());
        List<Integer> ids = failedToSend.stream().map(entity -> {
            publishMessage(entity.topic(), entity.json());
            return entity.id();
        }).toList();

        // if sending failed again, they are inserted into the DB by publishMessage() so we can delete all we just fetched
        failedToSendRepository.deleteAllById(ids);

        LOGGER.atDebug()
              .addArgument(streamingConfig.permissionId())
              .addArgument(failedToSend.size())
              .log("MqttStreamer for permission {} fetched and enqueued {} messages for sending that previously failed to send");
    }

    @Override
    public void authPacketArrived(int reasonCode, MqttProperties properties) {
        // Not needed
    }
}
