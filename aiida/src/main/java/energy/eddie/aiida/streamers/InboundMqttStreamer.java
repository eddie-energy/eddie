package energy.eddie.aiida.streamers;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.models.FailedToSendEntity;
import energy.eddie.aiida.models.permission.PermissionMqttConfig;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.repositories.FailedToSendRepository;
import energy.eddie.aiida.schemas.SchemaFormatter;
import energy.eddie.aiida.datasources.DataSourceMqttConfig;
import energy.eddie.dataneeds.needs.aiida.AiidaSchema;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class InboundMqttStreamer extends MqttStreamer {
    private static final Logger LOGGER = LoggerFactory.getLogger(InboundMqttStreamer.class);

//    private MqttConfig inboundStreamingConfig;
    private MqttAsyncClient dataClient;

    public InboundMqttStreamer(
            Permission permission,
            Flux<AiidaRecord> recordFlux,
            Sinks.One<UUID> terminationRequestSink,
            PermissionMqttConfig streamingConfig,
            DataSourceMqttConfig inboundStreamingConfig,
            MqttAsyncClient client,
            MqttAsyncClient dataClient,
            ObjectMapper mapper,
            FailedToSendRepository failedToSendRepository
    ) {
        super(permission, recordFlux, terminationRequestSink, streamingConfig, client, mapper, failedToSendRepository);

        this.inboundStreamingConfig = inboundStreamingConfig;
        this.dataClient = dataClient;

        dataClient.setCallback(this);
    }

    @Override
    public void connect() {
        super.connect();

        MqttConnectionOptions connOpts = new MqttConnectionOptions();
        connOpts.setCleanStart(false);
        connOpts.setAutomaticReconnect(true);
        connOpts.setAutomaticReconnectDelay(30, 60 * 5);
        connOpts.setUserName(inboundStreamingConfig.username());
        connOpts.setPassword(inboundStreamingConfig.password().getBytes(StandardCharsets.UTF_8));

        try {
            inboundClient.connect(connOpts);
        } catch (MqttException exception) {
            LOGGER.error("InboundMqttStreamer for permission {} has encountered an error while connecting to MQTT broker",
                         streamingConfig.permissionId(),
                         exception);
            return;
        }

        subscription = recordFlux.publishOn(Schedulers.boundedElastic()).subscribe(this::publishRecord);
    }

    @Override
    public void close() {
        super.close();

        try {
            inboundClient.disconnect(2000).waitForCompletion();
            inboundClient.close();
        } catch (MqttException e) {
            LOGGER.info("InboundMqttStreamer for permission {} has encountered an error while disconnecting/closing streamer",
                        streamingConfig.permissionId(),
                        e);
        }
    }

    private void publishRecord(AiidaRecord aiidaRecord) {
        LOGGER.trace("MqttStreamer for permission {} publishing AiidaRecord {}",
                     streamingConfig.permissionId(),
                     aiidaRecord);

        try {
            var schemas = (permission.dataNeed() == null) ? List.of(AiidaSchema.SMART_METER_P1_RAW) : permission.dataNeed()
                                                                                                                .schemas();

            for (var schema : schemas) {
                var schemaFormatter = SchemaFormatter.getFormatter(schema);
                var messageData = schemaFormatter.toSchema(aiidaRecord, mapper);
                publishMessage(inboundStreamingConfig.subscribeTopic(), messageData);
            }
        } catch (RuntimeException exception) {
            LOGGER.error("MqttStreamer for permission {} cannot convert AiidaRecord {} to JSON, will ignore it",
                         streamingConfig.permissionId(),
                         aiidaRecord,
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
            inboundClient.publish(topic, payload, 1, false);
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
}
