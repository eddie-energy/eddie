// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.streamers.mqtt;

import energy.eddie.aiida.errors.formatter.SchemaFormatterException;
import energy.eddie.aiida.errors.formatter.SchemaFormatterRegistryException;
import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.*;
import energy.eddie.aiida.repositories.FailedToSendRepository;
import energy.eddie.aiida.schemas.SchemaFormatterRegistry;
import energy.eddie.aiida.streamers.AiidaStreamer;
import energy.eddie.api.agnostic.aiida.AiidaConnectionStatusMessageDto;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import jakarta.annotation.Nullable;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class MqttStreamer extends AiidaStreamer implements MqttCallback {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttStreamer.class);
    private final MqttAsyncClient client;
    private final FailedToSendRepository failedToSendRepository;
    private final ObjectMapper mapper;
    private final Permission permission;
    private final MqttStreamingConfig streamingConfig;
    private final PermissionLatestRecordMap permissionLatestRecordMap;
    private boolean isBeingTerminated = false;
    @Nullable
    private Disposable subscription;

    /**
     * Creates a new MqttStreamer and initialized the client callback.
     *
     * @param failedToSendRepository  Repository where messages that could not be transmitted are stored.
     * @param mapper                  {@link ObjectMapper} used to transform the values to be sent into JSON strings.
     * @param permission              Permission for which this streamer is created.
     * @param recordFlux              Flux, where records that should be sent are published.
     * @param schemaFormatterRegistry Registry of all available schema formatters
     * @param streamingContext        Holds the {@link MqttAsyncClient} used to send to MQTT broker and the necessary
     *                                MQTT configuration values.
     * @param terminationRequestSink  Sink, to which the ID of the permission will be published when the EP requests a
     *                                termination.
     */
    public MqttStreamer(
            FailedToSendRepository failedToSendRepository,
            ObjectMapper mapper,
            Permission permission,
            Flux<AiidaRecord> recordFlux,
            SchemaFormatterRegistry schemaFormatterRegistry,
            MqttStreamingContext streamingContext,
            Sinks.One<UUID> terminationRequestSink
    ) {
        super(recordFlux, schemaFormatterRegistry, terminationRequestSink);

        this.client = streamingContext.client();
        this.failedToSendRepository = failedToSendRepository;
        this.mapper = mapper;
        this.permission = permission;
        this.streamingConfig = streamingContext.streamingConfig();
        this.permissionLatestRecordMap = streamingContext.permissionLatestRecordMap();

        client.setCallback(this);
    }

    @Override
    public void connect() {
        MqttConnectionOptions connOpts = new MqttConnectionOptions();
        connOpts.setCleanStart(false);
        connOpts.setAutomaticReconnect(true);
        connOpts.setAutomaticReconnectDelay(30, 60 * 5);
        connOpts.setUserName(streamingConfig.username().toString());
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

        subscription = recordFlux.publishOn(Schedulers.boundedElastic()).subscribe(this::publishRecord);
    }

    @Override
    public void close() {
        isBeingTerminated = true;
        if (subscription != null) subscription.dispose();
        terminationRequestSink.tryEmitEmpty();

        LOGGER.info("Closing MqttStreamer for permission {}", streamingConfig.permissionId());
        try {
            client.disconnect(2000).waitForCompletion();
            client.close();
        } catch (MqttException e) {
            LOGGER.info("MqttStreamer for permission {} has encountered an error while disconnecting/closing streamer",
                        streamingConfig.permissionId(),
                        e);
        }
    }

    @Override
    public void closeTerminally(AiidaConnectionStatusMessageDto statusMessage) {
        isBeingTerminated = true;
        if (subscription != null) subscription.dispose();
        LOGGER.atInfo()
              .addArgument(statusMessage.permissionId())
              .addArgument(statusMessage.status())
              .log("MqttStreamer for permission {} is requested to close with status {}");

        publishStatusMessageSynchronously(statusMessage);
        close();

        failedToSendRepository.deleteAllByPermissionId(streamingConfig.permissionId());
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
        LOGGER.info("Got termination message {}", message);
        var payload = new String(message.getPayload(), StandardCharsets.UTF_8);

        try {
            var permissionId = UUID.fromString(payload);
            if (permissionId.compareTo(streamingConfig.permissionId()) != 0) {
                throw new IllegalArgumentException();
            }

            isBeingTerminated = true;
            terminationRequestSink.emitValue(streamingConfig.permissionId(),
                                             Sinks.EmitFailureHandler.busyLooping(Duration.ofMinutes(3)));
        } catch (IllegalArgumentException e) {
            LOGGER.warn(
                    "MqttStreamer got request to terminate permission {}, but received wrong permission.permissionId() {}",
                    streamingConfig.permissionId(),
                    message.getPayload());
        }
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

    @Override
    public void authPacketArrived(int reasonCode, MqttProperties properties) {
        // Not needed
    }

    private void publishRecord(AiidaRecord aiidaRecord) {
        LOGGER.trace("MqttStreamer for permission {} publishing AiidaRecord {}",
                     streamingConfig.permissionId(),
                     aiidaRecord);

        try {
            var permissionLatestRecord = new PermissionLatestRecord(
                    streamingConfig.dataTopic(),
                    streamingConfig.serverUri()
            );

            var dataNeed = Objects.requireNonNull(permission.dataNeed());
            var schemas = Objects.requireNonNullElse(dataNeed.schemas(), Set.of(AiidaSchema.SMART_METER_P1_RAW));

            for (var schema : schemas) {
                var schemaFormatter = schemaFormatterRegistry.formatterFor(schema);
                var messageData = schemaFormatter.format(aiidaRecord, permission);
                publishMessage(schema.buildTopicPath(streamingConfig.dataTopic()), messageData);

                permissionLatestRecord.putSchema(schema, new LatestRecordSchema(
                        Instant.now(),
                        messageData != null ? new String(messageData, StandardCharsets.UTF_8) : ""
                ));
            }

            permissionLatestRecordMap.put(streamingConfig.permissionId(), permissionLatestRecord);
        } catch (SchemaFormatterException exception) {
            LOGGER.error("MqttStreamer for permission {} cannot convert AiidaRecord {} to JSON, will ignore it",
                         streamingConfig.permissionId(),
                         aiidaRecord,
                         exception);
        } catch (SchemaFormatterRegistryException e) {
            LOGGER.error("No SchemaFormatter for this permission {}, will ignore it",
                         streamingConfig.permissionId(),
                         e);
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
            // If a client is not connected, it will not save published messages to its persistence module, but instead
            // throws an exception. Therefore, we need to manually save messages that failed to send.
            client.publish(topic, payload, 1, false);
            LOGGER.atTrace()
                  .addArgument(streamingConfig.permissionId())
                  .addArgument(topic)
                  .log("MqttStreamer for permission {} successfully sent message to topic {}");
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

    private void publishStatusMessageSynchronously(AiidaConnectionStatusMessageDto statusMessage) {
        LOGGER.trace("MqttStreamer for permission {} synchronously publishing connectionStatusMessage {}",
                     streamingConfig.permissionId(),
                     statusMessage);

        try {
            byte[] jsonBytes = mapper.writeValueAsBytes(statusMessage);
            client.publish(streamingConfig.statusTopic(), jsonBytes, 1, true)
                  .waitForCompletion(Duration.ofMinutes(2).toMillis());
        } catch (JacksonException exception) {
            LOGGER.atError()
                  .addArgument(streamingConfig.permissionId())
                  .addArgument(statusMessage)
                  .setCause(exception)
                  .log("MqttStreamer for permission {} cannot convert ConnectionStatusMessage {} to JSON, will ignore it");
        } catch (MqttException exception) {
            LOGGER.atError()
                  .addArgument(streamingConfig.permissionId())
                  .addArgument(statusMessage)
                  .setCause(exception)
                  .log("MqttStreamer for permission {} failed to send ConnectionStatusMessage {}, will close streamer without retrying to send");
        }
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
        List<Long> ids = failedToSend.stream().map(entity -> {
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
}
