// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.shelly;

import energy.eddie.aiida.adapters.datasource.MqttDataSourceAdapter;
import energy.eddie.aiida.adapters.datasource.SmartMeterAdapterMeasurement;
import energy.eddie.aiida.adapters.datasource.shelly.transformer.ShellyComponent;
import energy.eddie.aiida.adapters.datasource.shelly.transformer.ShellyJson;
import energy.eddie.aiida.adapters.datasource.shelly.transformer.ShellyMeasurement;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.models.datasource.mqtt.shelly.ShellyDataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;

public class ShellyAdapter extends MqttDataSourceAdapter<ShellyDataSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShellyAdapter.class);
    private final ObjectMapper mapper;
    private Health healthState = Health.unknown().build();

    /**
     * Creates the datasource for the Shelly (energy meter) devices. It connects to the specified MQTT broker and expects
     * that the adapter publishes its JSON messages on the specified topic. Any OBIS code without a time field will be
     * assigned a Unix timestamp of 0.
     *
     * @param dataSource        The entity of the data source.
     * @param mapper            {@link ObjectMapper} that is used to deserialize the JSON messages.
     * @param mqttConfiguration The MQTT configuration that is used to connect to the MQTT broker.
     */
    public ShellyAdapter(
            ShellyDataSource dataSource,
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

        var payload = new String(message.getPayload(), StandardCharsets.UTF_8).trim();
        try {
            var json = mapper.readValue(payload, ShellyJson.class);

            var aiidaRecordValues = json.params().em()
                                        .entrySet()
                                        .stream()
                                        .flatMap(this::componentEntryToMeasurement)
                                        .map(SmartMeterAdapterMeasurement::toAiidaRecordValue)
                                        .toList();

            emitAiidaRecord(aiidaRecordValues);
        } catch (JacksonException e) {
            if (payload.equals("true") || payload.equals("false")) {
                var online = Boolean.parseBoolean(payload);
                setHealthState(online);
                return;
            }

            LOGGER.error("Error while deserializing payload received from adapter. Payload was {}", payload, e);
        }
    }

    @Override
    public Health health() {
        var health = super.health();
        if (healthState.getStatus().equals(Status.UNKNOWN)
            || (health != null && health.getStatus().equals(Status.DOWN))) {
            return health;
        }
        return healthState;
    }

    private Stream<ShellyMeasurement> componentEntryToMeasurement(
            Map.Entry<ShellyComponent, Map<String, Number>> componentEntry
    ) {
        return componentEntry.getValue()
                             .entrySet()
                             .stream()
                             .map(entry -> new ShellyMeasurement(
                                          componentEntry.getKey(),
                                          entry.getKey(),
                                          String.valueOf(entry.getValue())
                                  )
                             );
    }

    private void setHealthState(boolean online) {
        var status = online ? Status.UP : Status.DOWN;
        this.healthState = Health.status(status).build();

        LOGGER.info("Set health state of Shelly adapter {} to {}", dataSource.id(), status);
    }
}