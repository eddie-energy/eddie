package energy.eddie.aiida.adapters.datasource.at;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.adapters.datasource.MqttDataSourceAdapter;
import energy.eddie.aiida.adapters.datasource.SmartMeterAdapterMeasurement;
import energy.eddie.aiida.adapters.datasource.at.transformer.OesterreichsEnergieAdapterJson;
import energy.eddie.aiida.adapters.datasource.at.transformer.OesterreichsEnergieAdapterMeasurement;
import energy.eddie.aiida.adapters.datasource.at.transformer.OesterreichsEnergieAdapterValueDeserializer;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.models.datasource.mqtt.at.OesterreichsEnergieDataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class OesterreichsEnergieAdapter extends MqttDataSourceAdapter<OesterreichsEnergieDataSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OesterreichsEnergieAdapter.class);
    private final ObjectMapper mapper;

    /**
     * Creates the datasource for the Oesterreichs Energie adapter. It connects to the specified MQTT broker and expects
     * that the adapter publishes its JSON messages on the specified topic. Any OBIS code without a time field will be
     * assigned a Unix timestamp of 0.
     *
     * @param dataSource        The entity of the data source.
     * @param mapper            {@link ObjectMapper} that is used to deserialize the JSON messages. A
     *                          {@link OesterreichsEnergieAdapterValueDeserializer} will be registered to this mapper.
     * @param mqttConfiguration The MQTT configuration that is used to connect to the MQTT broker.
     */
    public OesterreichsEnergieAdapter(
            OesterreichsEnergieDataSource dataSource,
            ObjectMapper mapper,
            MqttConfiguration mqttConfiguration
    ) {
        super(dataSource, LOGGER, mqttConfiguration);
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
            var json = mapper.readValue(message.getPayload(), OesterreichsEnergieAdapterJson.class);

            List<AiidaRecordValue> aiidaRecordValues = json.energyData()
                                                           .entrySet()
                                                           .stream()
                                                           .map(entry ->
                                                                        new OesterreichsEnergieAdapterMeasurement(entry.getKey(),
                                                                                                                  String.valueOf(
                                                                                                                          entry.getValue()
                                                                                                                               .value()))
                                                           )
                                                           .map(SmartMeterAdapterMeasurement::toAiidaRecordValue)
                                                           .toList();

            emitAiidaRecord(dataSource.asset(), aiidaRecordValues);
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
                OesterreichsEnergieAdapter.class.getName(),
                token);
        throw new UnsupportedOperationException("The " + OesterreichsEnergieAdapter.class.getName() + " mustn't publish any MQTT messages");
    }
}