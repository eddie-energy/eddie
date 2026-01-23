// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.fr;

import energy.eddie.aiida.adapters.datasource.MqttDataSourceAdapter;
import energy.eddie.aiida.adapters.datasource.SmartMeterAdapterMeasurement;
import energy.eddie.aiida.adapters.datasource.fr.transformer.MicroTeleinfoV3DataFieldDeserializer;
import energy.eddie.aiida.adapters.datasource.fr.transformer.MicroTeleinfoV3Json;
import energy.eddie.aiida.adapters.datasource.fr.transformer.MicroTeleinfoV3Mode;
import energy.eddie.aiida.adapters.datasource.fr.transformer.MicroTeleinfoV3ModeNotSupportedException;
import energy.eddie.aiida.adapters.datasource.fr.transformer.history.HistoryModeEntry;
import energy.eddie.aiida.adapters.datasource.fr.transformer.history.MicroTeleinfoV3AdapterHistoryModeMeasurement;
import energy.eddie.aiida.adapters.datasource.fr.transformer.history.MicroTeleinfoV3HistoryModeJson;
import energy.eddie.aiida.adapters.datasource.fr.transformer.standard.MicroTeleinfoV3AdapterStandardModeMeasurement;
import energy.eddie.aiida.adapters.datasource.fr.transformer.standard.MicroTeleinfoV3StandardModeJson;
import energy.eddie.aiida.adapters.datasource.fr.transformer.standard.StandardModeEntry;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.models.datasource.mqtt.fr.MicroTeleinfoV3DataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.stream.Stream;

public class MicroTeleinfoV3Adapter extends MqttDataSourceAdapter<MicroTeleinfoV3DataSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MicroTeleinfoV3Adapter.class);
    private static final String HEALTH_TOPIC = "/status";
    private final String healthTopic;
    private final ObjectMapper mapper;
    private Health healthState = Health.unknown().withDetail(dataSource.id().toString(), "Initial value").build();
    private MicroTeleinfoV3Mode mode = MicroTeleinfoV3Mode.UNKNOWN;

    /**
     * Creates the datasource for the Micro Teleinfo V3. It connects to the specified MQTT broker and expects that the
     * adapter publishes its JSON messages on the specified topic. Any OBIS code without a time field will be assigned a
     * Unix timestamp of 0.
     *
     * @param dataSource        The entity of the data source.
     * @param mapper            {@link ObjectMapper} that is used to deserialize the JSON messages. A
     *                          {@link MicroTeleinfoV3DataFieldDeserializer} will be registered to this mapper.
     * @param mqttConfiguration The MQTT configuration that is used to connect to the MQTT broker.
     */
    public MicroTeleinfoV3Adapter(
            MicroTeleinfoV3DataSource dataSource,
            ObjectMapper mapper,
            MqttConfiguration mqttConfiguration
    ) {
        super(dataSource, LOGGER, mqttConfiguration);

        this.mapper = mapper;

        healthSink.asFlux().subscribe(this::setHealthState);
        this.healthTopic = dataSource.topic().replaceFirst("/#", HEALTH_TOPIC);
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
        try {
            LOGGER.trace("Topic {} new message: {}", topic, message);
            if (topic.endsWith(HEALTH_TOPIC)) {
                var status = message.toString();

                if (status.equalsIgnoreCase(Status.UP.toString())) {
                    emitNextHealthCheck(Status.UP);
                    return;
                }

                emitNextHealthCheck(Status.DOWN);
                return;
            }

            if (mode.equals(MicroTeleinfoV3Mode.UNKNOWN)) {
                mode = determineMode(message.getPayload());
                LOGGER.debug("Connected smart meter operates in {} mode.", mode);
            }

            List<AiidaRecordValue> aiidaRecordValues = switch (mode) {
                case HISTORY -> {
                    var historyModeData = readPayload(message.getPayload(), MicroTeleinfoV3HistoryModeJson.class)
                            .energyData();

                    var historyModeMeasurementsStream = historyModeData
                            .entrySet()
                            .stream()
                            .map(entry ->
                                         new MicroTeleinfoV3AdapterHistoryModeMeasurement(
                                                 entry.getKey(),
                                                 String.valueOf(
                                                         entry.getValue()
                                                              .value())));

                    var positiveActiveInstantaneousPower = MicroTeleinfoV3AdapterHistoryModeMeasurement
                            .calculateAiidaPositiveActiveInstantaneousPowerFromHistoryModeData(historyModeData)
                            .stream();
                    var positiveActiveEnergy = MicroTeleinfoV3AdapterHistoryModeMeasurement
                            .calculateAiidaPositiveActiveEnergyFromHistoryModeData(historyModeData)
                            .stream();

                    yield Stream.of(historyModeMeasurementsStream,
                                    positiveActiveEnergy,
                                    positiveActiveInstantaneousPower)
                                .flatMap(stream -> stream)
                                .map(SmartMeterAdapterMeasurement::toAiidaRecordValue)
                                .toList();
                }
                case STANDARD -> readPayload(message.getPayload(), MicroTeleinfoV3StandardModeJson.class)
                        .energyData()
                        .entrySet()
                        .stream()
                        .map(entry ->
                                     new MicroTeleinfoV3AdapterStandardModeMeasurement(
                                             entry.getKey(),
                                             String.valueOf(entry.getValue().sanitizedValue(entry.getKey())))
                        ).map(SmartMeterAdapterMeasurement::toAiidaRecordValue)
                        .toList();
                case UNKNOWN -> throw new MicroTeleinfoV3ModeNotSupportedException(message.getPayload(),
                                                                                   List.of(MicroTeleinfoV3Mode.UNKNOWN));
            };

            LOGGER.trace("{} mode message ({} values) deserialized successfully.", mode, aiidaRecordValues.size());
            emitAiidaRecord(dataSource.asset(), aiidaRecordValues);
        } catch (MicroTeleinfoV3ModeNotSupportedException e) {
            LOGGER.error("Error while deserializing JSON received from adapter. JSON was {}",
                         e.payload(),
                         e);
        }
    }

    @Override
    public Health health() {
        var health = super.health();
        if (health != null && health.getStatus().equals(Status.DOWN)) {
            return health;
        }
        return healthState;
    }

    @Override
    protected void subscribeToHealthTopic() {
        LOGGER.info("Will subscribe to health topic {}", healthTopic);

        try {
            if (asyncClient != null) {
                asyncClient.subscribe(healthTopic, 1);
            }
        } catch (MqttException ex) {
            LOGGER.error("Error while subscribing to topic {}", healthTopic, ex);
            healthSink.tryEmitNext(Health.down().withDetail("Error", ex).build());
        }
    }

    private MicroTeleinfoV3Mode determineMode(byte[] payload) throws MicroTeleinfoV3ModeNotSupportedException {
        try {
            LOGGER.debug("Checking type of incoming payload.");
            var rootNode = mapper.readTree(payload);

            if (rootNode.has(HistoryModeEntry.ADCO.toString())) {
                LOGGER.debug("Payload is a {}", MicroTeleinfoV3HistoryModeJson.class.getSimpleName());
                return MicroTeleinfoV3Mode.HISTORY;
            }

            if (rootNode.has(StandardModeEntry.ADSC.toString())) {
                LOGGER.debug("Payload is a {}", MicroTeleinfoV3StandardModeJson.class.getSimpleName());
                return MicroTeleinfoV3Mode.STANDARD;
            }

            throw new MicroTeleinfoV3ModeNotSupportedException(payload,
                                                               List.of(MicroTeleinfoV3Mode.HISTORY,
                                                                       MicroTeleinfoV3Mode.STANDARD));
        } catch (JacksonException e) {
            throw new MicroTeleinfoV3ModeNotSupportedException(payload,
                                                               List.of(MicroTeleinfoV3Mode.HISTORY,
                                                                       MicroTeleinfoV3Mode.STANDARD));
        }
    }

    private MicroTeleinfoV3Json readPayload(
            byte[] payload,
            Class<? extends MicroTeleinfoV3Json> type
    ) throws MicroTeleinfoV3ModeNotSupportedException {
        try {
            return mapper.readValue(payload, type);
        } catch (JacksonException e) {
            if (mode.equals(MicroTeleinfoV3Mode.UNKNOWN)) {
                throw new MicroTeleinfoV3ModeNotSupportedException(payload,
                                                                   List.of(MicroTeleinfoV3Mode.HISTORY,
                                                                           MicroTeleinfoV3Mode.STANDARD));
            }

            throw new MicroTeleinfoV3ModeNotSupportedException(payload, List.of(mode));
        }
    }

    /**
     * Sets the health state to either up or down depending on the status sent by the mqtt broker
     *
     * @param healthState UP or DOWN
     */
    private void setHealthState(Health healthState) {
        this.healthState = healthState;
    }

    private void emitNextHealthCheck(Status status) {
        if (status.equals(Status.UP) && !healthState.getStatus().equals(Status.UP)) {
            healthSink.tryEmitNext(Health.up().build());
        } else if (status.equals(Status.DOWN) && !healthState.getStatus().equals(Status.DOWN)) {
            healthSink.tryEmitNext(Health.down()
                                         .withDetail(dataSource().id().toString(),
                                                     "The datasource is not working properly.")
                                         .build());
        }
    }
}