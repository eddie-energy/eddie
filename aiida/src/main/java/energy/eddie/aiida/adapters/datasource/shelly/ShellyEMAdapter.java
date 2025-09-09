package energy.eddie.aiida.adapters.datasource.shelly;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.adapters.datasource.MqttDataSourceAdapter;
import energy.eddie.aiida.adapters.datasource.SmartMeterAdapterMeasurement;
import energy.eddie.aiida.adapters.datasource.shelly.transformer.ShellyEMComponent;
import energy.eddie.aiida.adapters.datasource.shelly.transformer.ShellyEMJson;
import energy.eddie.aiida.adapters.datasource.shelly.transformer.ShellyEMMeasurement;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.models.datasource.mqtt.shelly.ShellyEMDataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;

public class ShellyEMAdapter extends MqttDataSourceAdapter<ShellyEMDataSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShellyEMAdapter.class);
    private final ObjectMapper mapper;

    /**
     * Creates the datasource for the Shelly EM (energy meter) devices. It connects to the specified MQTT broker and expects
     * that the adapter publishes its JSON messages on the specified topic. Any OBIS code without a time field will be
     * assigned a Unix timestamp of 0.
     *
     * @param dataSource        The entity of the data source.
     * @param mapper            {@link ObjectMapper} that is used to deserialize the JSON messages.
     * @param mqttConfiguration The MQTT configuration that is used to connect to the MQTT broker.
     */
    public ShellyEMAdapter(
            ShellyEMDataSource dataSource,
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
            var json = mapper.readValue(message.getPayload(), ShellyEMJson.class);

            var aiidaRecordValues = json.params().em()
                                        .entrySet()
                                        .stream()
                                        .flatMap(this::componentEntryToMeasurement)
                                        .map(SmartMeterAdapterMeasurement::toAiidaRecordValue)
                                        .toList();

            emitAiidaRecord(dataSource.asset(), aiidaRecordValues);
        } catch (IOException e) {
            LOGGER.error("Error while deserializing JSON received from adapter. JSON was {}",
                         new String(message.getPayload(), StandardCharsets.UTF_8),
                         e);
        }
    }

    private Stream<ShellyEMMeasurement> componentEntryToMeasurement(
            Map.Entry<ShellyEMComponent, Map<String, Number>> componentEntry
    ) {
        return componentEntry.getValue()
                             .entrySet()
                             .stream()
                             .map(entry -> new ShellyEMMeasurement(
                                          componentEntry.getKey(),
                                          entry.getKey(),
                                          String.valueOf(entry.getValue())
                                  )
                             );
    }
}