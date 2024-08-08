package energy.eddie.aiida.datasources.fr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import energy.eddie.aiida.datasources.MqttDataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordFactory;
import energy.eddie.aiida.utils.MqttConfig;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class MicroTeleinfoV3 extends MqttDataSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(MicroTeleinfoV3.class);
    private final ObjectMapper mapper;

    /**
     * Creates the datasource for the Micro Teleinfo V3. It connects to the specified MQTT broker and expects that the
     * adapter publishes its JSON messages on the specified topic. Any OBIS code without a time field will be assigned a
     * Unix timestamp of 0.
     *
     * @param mqttConfig Configuration detailing the MQTT broker to connect to and options to use.
     * @param mapper     {@link ObjectMapper} that is used to deserialize the JSON messages. A
     *                   {@link MicroTeleinfoV3ValueDeserializer} will be registered to this mapper.
     */
    public MicroTeleinfoV3(String dataSourceId, MqttConfig mqttConfig, ObjectMapper mapper) {
        super(dataSourceId, "MicroTeleinfoV3", mqttConfig, LOGGER);

        SimpleModule module = new SimpleModule();
        module.addDeserializer(MicroTeleinfoV3Json.TeleinfoDataField.class,
                               new MicroTeleinfoV3ValueDeserializer(null));
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
            var json = mapper.readValue(message.getPayload(), MicroTeleinfoV3Json.class);

            // TODO: Rework with GH-1209 to support other kinds of data supplied by MicroTeleinfoV3
            var energyData = json.papp();
            emitNextAiidaRecord("1-0:1.7.0", energyData);
        } catch (IOException e) {
            LOGGER.error("Error while deserializing JSON received from adapter. JSON was {}",
                         new String(message.getPayload(), StandardCharsets.UTF_8), e);
        }
    }

    private void emitNextAiidaRecord(String code, MicroTeleinfoV3Json.TeleinfoDataField entry) {
        try {
            Instant timestamp = Instant.now();

            var aiidaRecord = AiidaRecordFactory.createRecord(code, timestamp, entry.value());
            var result = recordSink.tryEmitNext(aiidaRecord);

            if (result.isFailure())
                LOGGER.error("Error while emitting new AiidaRecord {}. Error was {}", aiidaRecord, result);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Got code {} from Teleinfo, but AiidaRecordFactory does not know how to handle it", code, e);
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
                MicroTeleinfoV3.class.getName(),
                token
        );
        throw new UnsupportedOperationException("The " + MicroTeleinfoV3.class.getName() + " mustn't publish any MQTT messages");
    }
}