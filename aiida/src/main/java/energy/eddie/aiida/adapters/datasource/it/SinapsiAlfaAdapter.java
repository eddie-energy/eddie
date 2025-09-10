package energy.eddie.aiida.adapters.datasource.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.adapters.datasource.MqttDataSourceAdapter;
import energy.eddie.aiida.adapters.datasource.SmartMeterAdapterMeasurement;
import energy.eddie.aiida.adapters.datasource.it.transformer.SinapsiAlfaJson;
import energy.eddie.aiida.adapters.datasource.it.transformer.SinapsiAlfaMeasurement;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.models.datasource.mqtt.it.SinapsiAlfaDataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SinapsiAlfaAdapter extends MqttDataSourceAdapter<SinapsiAlfaDataSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SinapsiAlfaAdapter.class);
    private final ObjectMapper mapper;

    /**
     * Creates the datasource for the Sinapsi ALFA adapter. It connects to the specified MQTT broker and expects
     * that the adapter publishes its JSON messages on the specified topic. Any OBIS code without a time field will be
     * assigned a Unix timestamp of 0.
     *
     * @param dataSource        The entity of the data source.
     * @param mapper            {@link ObjectMapper} that is used to deserialize the JSON messages.
     * @param mqttConfiguration The MQTT configuration that is used to connect to the MQTT broker.
     */
    public SinapsiAlfaAdapter(
            SinapsiAlfaDataSource dataSource,
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
            var json = mapper.readValue(message.getPayload(), SinapsiAlfaJson.class);

            // TODO: buffer values

            List<AiidaRecordValue> aiidaRecordValues = json.data()
                                                           .stream()
                                                           .flatMap(dataEntry -> dataEntry.entries()
                                                                                          .entrySet()
                                                                                          .stream()
                                                           )
                                                           .map(entry -> new SinapsiAlfaMeasurement(
                                                                        entry.getKey(),
                                                                        String.valueOf(entry.getValue())
                                                                )
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
}