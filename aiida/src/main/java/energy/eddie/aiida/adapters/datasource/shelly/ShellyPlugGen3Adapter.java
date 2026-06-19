// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.shelly;

import energy.eddie.aiida.adapters.datasource.MqttDataSourceAdapter;
import energy.eddie.aiida.adapters.datasource.SmartMeterAdapterMeasurement;
import energy.eddie.aiida.adapters.datasource.shelly.transformer.ShellyPlugGen3Json;
import energy.eddie.aiida.adapters.datasource.shelly.transformer.ShellyPlugGen3Measurement;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.models.datasource.mqtt.shelly.ShellyPlugGen3DataSource;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ShellyPlugGen3Adapter extends MqttDataSourceAdapter<ShellyPlugGen3DataSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShellyPlugGen3Adapter.class);
    private final ObjectMapper mapper;

    /**
     * Creates the datasource for the Shelly Gen3 Plug devices. It connects to the specified MQTT broker and expects
     * that the adapter publishes its JSON messages on the specified topic. Shelly Gen3 devices use the RPC-over-MQTT
     * structure with measurements grouped under switch:0 components.
     *
     * @param dataSource        The entity of the data source.
     * @param mapper            {@link ObjectMapper} that is used to deserialize the JSON messages.
     * @param mqttConfiguration The MQTT configuration that is used to connect to the MQTT broker.
     */
    public ShellyPlugGen3Adapter(
            ShellyPlugGen3DataSource dataSource,
            ObjectMapper mapper,
            MqttConfiguration mqttConfiguration
    ) {
        super(dataSource, LOGGER, mqttConfiguration);
        this.mapper = mapper;
    }

    /**
     * MQTT callback function that is called when a new message from the broker is received. Will convert the message to
     * {@link energy.eddie.aiida.models.record.AiidaRecord}s and publish them on the Flux returned by {@link #start()}.
     *
     * @param topic   Name of the topic, the message was published to.
     * @param message The actual message.
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        LOGGER.trace("Topic {} new message: {}", topic, message);

        var payload = new String(message.getPayload(), StandardCharsets.UTF_8).trim();
        try {
            var json = mapper.readValue(payload, ShellyPlugGen3Json.class);

            if (json.params() == null || json.params().switch0() == null) {
                return;
            }

            // Only process RPC notifications that contain measurements - typically NotifyStatus/NotifyEvent
            var method = json.method();
            if (method == null || !("NotifyStatus".equals(method) || "NotifyEvent".equals(method))) {
                return;
            }

            var switchData = json.params().switch0();
            List<ShellyPlugGen3Measurement> measurements = new ArrayList<>();

            if (switchData.apower() != null) {
                measurements.add(new ShellyPlugGen3Measurement(
                        "switch:0:apower",
                        String.valueOf(switchData.apower()),
                        ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER,
                        UnitOfMeasurement.WATT
                ));
            }
            // map the output state (on/off) if present
            if (switchData.output() != null) {
                measurements.add(new ShellyPlugGen3Measurement(
                        "switch:0:output",
                        String.valueOf(switchData.output()),
                        ObisCode.UNKNOWN,
                        UnitOfMeasurement.NONE
                ));
            }
            if (switchData.voltage() != null) {
                measurements.add(new ShellyPlugGen3Measurement(
                        "switch:0:voltage",
                        String.valueOf(switchData.voltage()),
                        ObisCode.INSTANTANEOUS_VOLTAGE,
                        UnitOfMeasurement.VOLT
                ));
            }
            if (switchData.current() != null) {
                measurements.add(new ShellyPlugGen3Measurement(
                        "switch:0:current",
                        String.valueOf(switchData.current()),
                        ObisCode.INSTANTANEOUS_CURRENT,
                        UnitOfMeasurement.AMPERE
                ));
            }
            if (switchData.freq() != null) {
                measurements.add(new ShellyPlugGen3Measurement(
                        "switch:0:freq",
                        String.valueOf(switchData.freq()),
                        ObisCode.FREQUENCY,
                        UnitOfMeasurement.HERTZ
                ));
            }
            if (switchData.aenergy() != null && switchData.aenergy().total() != null) {
                measurements.add(new ShellyPlugGen3Measurement(
                        "switch:0:aenergy.total",
                        String.valueOf(switchData.aenergy().total()),
                        ObisCode.POSITIVE_ACTIVE_ENERGY,
                        UnitOfMeasurement.WATT_HOUR
                ));
            }

            var aiidaRecordValues = measurements.stream()
                    .map(SmartMeterAdapterMeasurement::toAiidaRecordValue)
                    .filter(Objects::nonNull)
                    .toList();

            if (!aiidaRecordValues.isEmpty()) {
                emitAiidaRecord(aiidaRecordValues);
            }
        } catch (JacksonException e) {
            LOGGER.error("Error while deserializing payload received from Gen3 adapter. Payload was {}", payload, e);
        }
    }
}
