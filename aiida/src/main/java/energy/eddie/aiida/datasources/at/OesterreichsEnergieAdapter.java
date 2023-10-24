package energy.eddie.aiida.datasources.at;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import energy.eddie.aiida.datasources.AiidaDataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordFactory;
import energy.eddie.aiida.utils.MqttConfig;
import energy.eddie.aiida.utils.MqttFactory;
import jakarta.annotation.Nullable;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Instant;

public class OesterreichsEnergieAdapter extends AiidaDataSource implements MqttCallbackExtended {
    private static final Logger LOGGER = LoggerFactory.getLogger(OesterreichsEnergieAdapter.class);
    private final MqttConfig mqttConfig;
    private final ObjectMapper mapper;
    private final String clientId;
    @Nullable
    private MqttAsyncClient asyncClient;

    /**
     * Creates the datasource for the Oesterreichs Energie adapter.
     * It connects to the specified MQTT broker and expects that the adapter publishes its JSON messages on the
     * specified topic.
     * Any OBIS code without a time field will be assigned a Unix timestamp of 0.
     *
     * @param mqttConfig Configuration detailing the MQTT broker to connect to and options to use.
     * @param mapper     {@link ObjectMapper} that is used to deserialize the JSON messages.
     *                   A {@link OesterreichsEnergieAdapterValueDeserializer} will be registered to this mapper.
     */
    public OesterreichsEnergieAdapter(MqttConfig mqttConfig, ObjectMapper mapper) {
        super("Oesterreichs Energie Adapter (SMA)");
        this.mqttConfig = mqttConfig;
        this.mapper = mapper;

        clientId = "AIIDA";

        SimpleModule module = new SimpleModule();
        module.addDeserializer(OesterreichAdapterJson.AdapterValue.class,
                new OesterreichsEnergieAdapterValueDeserializer(null));
        mapper.registerModule(module);
    }

    /**
     * Start the listening or active polling for data from the datasource.
     * The {@code time} field from the adapter is interpreted as UTC unix timestamp.
     */
    @Override
    public Flux<AiidaRecord> start() {
        LOGGER.info("Starting {}", name());

        try {
            // persistence is only required when publishing messages with QOS 1 or 2
            asyncClient = MqttFactory.getMqttAsyncClient(mqttConfig.serverURI(), clientId, null);
            asyncClient.setCallback(this);

            MqttConnectOptions connectOptions = createConnectOptions();

            LOGGER.info("Connecting to broker {}", mqttConfig.serverURI());

            asyncClient.connect(connectOptions);
        } catch (MqttException ex) {
            LOGGER.error("Error while connecting to MQTT server {} for {}", mqttConfig.serverURI(), name(), ex);

            recordSink.tryEmitError(ex);
        }

        return recordSink.asFlux();
    }

    private MqttConnectOptions createConnectOptions() {
        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setCleanSession(mqttConfig.cleanSession());
        connectOptions.setAutomaticReconnect(mqttConfig.automaticReconnect());
        connectOptions.setKeepAliveInterval(mqttConfig.keepAliveInterval());

        if (mqttConfig.username() != null && mqttConfig.password() != null) {
            connectOptions.setUserName(mqttConfig.username());
            connectOptions.setPassword(mqttConfig.password().toCharArray());
        }
        return connectOptions;
    }

    /**
     * Close any open connections and free resources used by this class.
     * Also emit a complete signal on the {@code recordSink} of this datasource.
     */
    @Override
    public void close() {
        LOGGER.info("Closing {}", name());

        if (asyncClient != null) {
            try {
                if (asyncClient.isConnected())
                    asyncClient.disconnect(1000L * 30);
                asyncClient.close();
            } catch (MqttException ex) {
                LOGGER.warn("Error while disconnecting or closing MQTT client", ex);
            }
        }

        recordSink.tryEmitComplete();
    }

    @Override
    public void connectionLost(Throwable cause) {
        LOGGER.warn("Lost connection to MQTT broker", cause);
    }

    /**
     * MQTT callback function that is called when a new message from the broker is received.
     * Will convert the message to {@link AiidaRecord}s and publish them on the Flux returned by {@link #start()}.
     *
     * @param topic   Name of the topic, the message was published to.
     * @param message The actual message.
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        LOGGER.trace("Topic {} new message: {}", topic, message);

        try {
            var json = mapper.readValue(message.getPayload(), OesterreichAdapterJson.class);

            for (String code : json.energyData().keySet()) {
                var entry = json.energyData().get(code);
                Instant timestamp = Instant.ofEpochMilli(entry.time());

                var aiidaRecord = AiidaRecordFactory.createRecord(code, timestamp, entry.value());
                var result = recordSink.tryEmitNext(aiidaRecord);

                if (result.isFailure())
                    LOGGER.error("Error while emitting new AiidaRecord {}. Error was {}", aiidaRecord, result);
            }
        } catch (IOException e) {
            LOGGER.error("Error while deserializing JSON received from adapter. JSON was {}", new String(message.getPayload()), e);
        }
    }

    /**
     * Called when the connection to the broker has been established and will then subscribe to the topic specified in {@code mqttConfig}.
     *
     * @param reconnect If true, the connection was the result of automatic reconnect.
     * @param serverURI The server URI that the connection was made to.
     */
    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        LOGGER.info("{} connected successfully to broker {}, was from automatic reconnect is {}", name(), serverURI, reconnect);
        LOGGER.info("Will subscribe to topic {}", mqttConfig.subscribeTopic());

        try {
            if (asyncClient != null)
                asyncClient.subscribe(mqttConfig.subscribeTopic(), 2);
        } catch (MqttException ex) {
            LOGGER.error("Error while subscribing to topic {}", mqttConfig.subscribeTopic(), ex);
            recordSink.tryEmitError(ex);
        }
    }

    /**
     * Will always throw {@link UnsupportedOperationException}, as this datasource is not designed to publish data.
     *
     * @param token The delivery token associated with the message.
     * @throws UnsupportedOperationException Always thrown, as this datasource is not designed to publish data.
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) throws UnsupportedOperationException {
        LOGGER.warn("Got deliveryComplete notification, but OesterreichsEnergieAdapter mustn't publish any MQTT messages but just listen. Token was {}", token);
        throw new UnsupportedOperationException("The OesterreichsEnergieAdapter mustn't publish any MQTT messages");
    }
}
