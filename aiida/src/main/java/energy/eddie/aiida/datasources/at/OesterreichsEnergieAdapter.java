package energy.eddie.aiida.datasources.at;

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

import static energy.eddie.aiida.datasources.at.OesterreichAdapterJson.AdapterValue;

public class OesterreichsEnergieAdapter extends MqttDataSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(OesterreichsEnergieAdapter.class);
    private final ObjectMapper mapper;

    /**
     * Creates the datasource for the Oesterreichs Energie adapter. It connects to the specified MQTT broker and expects
     * that the adapter publishes its JSON messages on the specified topic. Any OBIS code without a time field will be
     * assigned a Unix timestamp of 0.
     *
     * @param mqttConfig Configuration detailing the MQTT broker to connect to and options to use.
     * @param mapper     {@link ObjectMapper} that is used to deserialize the JSON messages. A
     *                   {@link OesterreichsEnergieAdapterValueDeserializer} will be registered to this mapper.
     */
    public OesterreichsEnergieAdapter(String dataSourceId, MqttConfig mqttConfig, ObjectMapper mapper) {
        super(dataSourceId, "Oesterreichs Energie Adapter (SMA)", mqttConfig, LOGGER);
        SimpleModule module = new SimpleModule();
        module.addDeserializer(OesterreichAdapterJson.AdapterValue.class,
                               new OesterreichsEnergieAdapterValueDeserializer(null));
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
            var json = mapper.readValue(message.getPayload(), OesterreichAdapterJson.class);

            var energyData = json.energyData();
            for (var entry : energyData.entrySet()) {
                emitNextAiidaRecord(entry.getKey(), entry.getValue());
            }
        } catch (IOException e) {
            LOGGER.error("Error while deserializing JSON received from adapter. JSON was {}",
                         new String(message.getPayload(), StandardCharsets.UTF_8), e);
        }
    }

    private void emitNextAiidaRecord(String code, AdapterValue entry) {
        try {
            Instant timestamp = Instant.ofEpochSecond(entry.time());

            var aiidaRecord = AiidaRecordFactory.createRecord(code, timestamp, entry.value());
            var result = recordSink.tryEmitNext(aiidaRecord);

            if (result.isFailure())
                LOGGER.error("Error while emitting new AiidaRecord {}. Error was {}", aiidaRecord, result);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Got OBIS code {} from SMA, but AiidaRecordFactory does not know how to handle it", code, e);
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
                OesterreichsEnergieAdapter.class.getName(),
                token
        );
        throw new UnsupportedOperationException("The " + OesterreichsEnergieAdapter.class.getName() + " mustn't publish any MQTT messages");
    }
}