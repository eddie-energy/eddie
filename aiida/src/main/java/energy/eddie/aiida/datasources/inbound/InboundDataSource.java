package energy.eddie.aiida.datasources.inbound;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import energy.eddie.aiida.datasources.AiidaMqttDataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.utils.MqttConfig;
import energy.eddie.aiida.utils.ObisCode;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class InboundDataSource extends AiidaMqttDataSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(InboundDataSource.class);
    private final ObjectMapper mapper;

    /**
     * Creates the datasource for the inbound data. It connects to the specified MQTT broker and expects
     * that the adapter publishes its JSON messages on the specified topic. Any OBIS code without a time field will be
     * assigned a Unix timestamp of 0.
     *
     * @param permissionId The unique identifier (UUID) for the permission of this data source.
     * @param userId       The ID of the user who owns this data source.
     * @param mqttConfig   Configuration detailing the MQTT broker to connect to and options to use.
     * @param mapper       {@link ObjectMapper} that is used to deserialize the JSON messages.
     */
    public InboundDataSource(UUID permissionId, UUID userId, MqttConfig mqttConfig, ObjectMapper mapper) {
        super(permissionId, userId, "EligiblePartyAdapter", mqttConfig, LOGGER);
        SimpleModule module = new SimpleModule();
        mapper.registerModule(module);
        this.mapper = mapper;
    }

    /**
     * MQTT callback function that is called when a new message from the broker is received. Will convert the message to
     * {@link AiidaRecord}s and publish them on the Flux returned by {@link #start()}.
     *
     * @param topic   Name of the topic, the message was published to.
     * @param message The actual message.
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        LOGGER.trace("Topic {} new message: {}", topic, message);

        try {
            var json = mapper.readValue(message.getPayload(), InboundDataSourceJson.class);

            var value = String.valueOf(json.value());
            var aiidaRecord = new AiidaRecordValue(json.code(),
                                                   ObisCode.forCode(json.code()),
                                                   value,
                                                   json.unit(),
                                                   value,
                                                   json.unit());

            emitAiidaRecord(AiidaAsset.CONNECTION_AGREEMENT_POINT.toString(), List.of(aiidaRecord));
        } catch (IOException e) {
            LOGGER.error("Error while deserializing JSON received from adapter. JSON was {}",
                         new String(message.getPayload(), StandardCharsets.UTF_8),
                         e);
        }
    }

    /**
     * Will always throw {@link UnsupportedOperationException}, as this datasource is not designed to publish data.
     *
     * @param token The delivery token associated with the message.
     * @throws UnsupportedOperationException Always thrown, as this datasource is not designed to publish data.
     */
    @Override
    public void deliveryComplete(IMqttToken token) throws UnsupportedOperationException {
        LOGGER.warn(
                "Got deliveryComplete notification, but {} mustn't publish any MQTT messages but just listen. Token was {}",
                InboundDataSource.class.getName(),
                token);
        throw new UnsupportedOperationException("The " + InboundDataSource.class.getName() + " mustn't publish any MQTT messages");
    }
}