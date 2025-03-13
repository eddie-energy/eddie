package energy.eddie.aiida.adapters.datasource.fr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import energy.eddie.aiida.adapters.datasource.MqttDataSourceAdapter;
import energy.eddie.aiida.models.datasource.fr.MicroTeleinfoV3DataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MicroTeleinfoV3Adapter extends MqttDataSourceAdapter<MicroTeleinfoV3DataSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MicroTeleinfoV3Adapter.class);
    private static final String HEALTH_TOPIC = "/status";
    private final String healthTopic;
    private final ObjectMapper mapper;
    private Health healthState = Health.unknown().withDetail(dataSource.id().toString(), "Initial value").build();

    /**
     * Creates the datasource for the Micro Teleinfo V3. It connects to the specified MQTT broker and expects that the
     * adapter publishes its JSON messages on the specified topic. Any OBIS code without a time field will be assigned a
     * Unix timestamp of 0.
     *
     * @param dataSource The entity of the data source.
     * @param mapper     {@link ObjectMapper} that is used to deserialize the JSON messages. A
     *                   {@link MicroTeleinfoV3AdapterValueDeserializer} will be registered to this mapper.
     */
    public MicroTeleinfoV3Adapter(MicroTeleinfoV3DataSource dataSource, ObjectMapper mapper) {
        super(dataSource, LOGGER);

        SimpleModule module = new SimpleModule();
        module.addDeserializer(MicroTeleinfoV3AdapterJson.TeleinfoDataField.class,
                               new MicroTeleinfoV3AdapterValueDeserializer(null));
        mapper.registerModule(module);
        this.mapper = mapper;

        healthSink.asFlux().subscribe(this::setHealthState);
        this.healthTopic = dataSource.mqttSubscribeTopic().replaceFirst("/.*", HEALTH_TOPIC);
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
        if (topic.endsWith(HEALTH_TOPIC)) {
            var status = message.toString();

            if (status.equalsIgnoreCase(Status.UP.toString())) {
                emitNextHealthCheck(Status.UP);
            } else if (status.equalsIgnoreCase(Status.DOWN.toString())) {
                emitNextHealthCheck(Status.DOWN);
            }
        } else {
            try {
                var json = mapper.readValue(message.getPayload(), MicroTeleinfoV3AdapterJson.class);

                // TODO: Rework with GH-1209 to support other kinds of data supplied by MicroTeleinfoV3
                List<AiidaRecordValue> aiidaRecordValues = new ArrayList<>();
                var papp = json.papp();
                var pappValue = String.valueOf(papp.value());
                var base = json.base();
                var baseValue = String.valueOf(base.value());

                aiidaRecordValues.add(new AiidaRecordValue("PAPP",
                                                           papp.mappedObisCode(),
                                                           pappValue,
                                                           papp.unitOfMeasurement(),
                                                           pappValue,
                                                           papp.unitOfMeasurement()));
                aiidaRecordValues.add(new AiidaRecordValue("BASE",
                                                           base.mappedObisCode(),
                                                           baseValue,
                                                           base.unitOfMeasurement(),
                                                           baseValue,
                                                           base.unitOfMeasurement()));

                emitAiidaRecord(dataSource.asset(), aiidaRecordValues);
            } catch (IOException e) {
                LOGGER.error("Error while deserializing JSON received from adapter. JSON was {}",
                             new String(message.getPayload(), StandardCharsets.UTF_8),
                             e);
            }
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
                MicroTeleinfoV3Adapter.class.getName(),
                token);
        throw new UnsupportedOperationException("The " + MicroTeleinfoV3Adapter.class.getName() + " mustn't publish any MQTT messages");
    }

    @Override
    public Health health() {
        if (super.health().getStatus().equals(Status.DOWN)) {
            return super.health();
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