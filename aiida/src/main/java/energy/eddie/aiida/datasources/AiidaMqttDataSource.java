package energy.eddie.aiida.datasources;

import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.utils.MqttConfig;
import energy.eddie.aiida.utils.MqttFactory;
import jakarta.annotation.Nullable;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.Logger;
import org.springframework.boot.actuate.health.Health;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

public abstract class AiidaMqttDataSource extends AiidaDataSource implements MqttCallback {
    private static final Duration DISCONNECT_TIMEOUT = Duration.ofSeconds(30);
    private final Logger logger;
    private final MqttConfig mqttConfig;
    private final UUID clientId;
    @Nullable
    protected MqttAsyncClient asyncClient;

    /**
     * Creates a new {@code MqttDataSource} with the specified display name and mqttConfig.
     *
     * @param id         ID of the datasource
     * @param name       Display name of this new datasource.
     * @param mqttConfig The mqttConfiguration for the specified data source
     */
    protected AiidaMqttDataSource(
            UUID id,
            UUID userId,
            String name,
            MqttConfig mqttConfig,
            Logger childLogger
    ) {
        super(id, userId, name);
        this.mqttConfig = mqttConfig;
        clientId = UUID.randomUUID();
        logger = childLogger;
    }

    /**
     * Start the listening or active polling for data from the datasource. The {@code time} field from the adapter is
     * interpreted as unix timestamp.
     */
    @Override
    public Flux<AiidaRecord> start() {
        logger.info("Starting {}", name());

        try {
            // persistence is only required when publishing messages with QOS 1 or 2
            asyncClient = MqttFactory.getMqttAsyncClient(mqttConfig.serverURI(), clientId.toString(), null);
            asyncClient.setCallback(this);

            MqttConnectionOptions connectOptions = createConnectOptions();

            logger.info("Connecting to broker {}", mqttConfig.serverURI());

            asyncClient.connect(connectOptions);
        } catch (MqttException ex) {
            logger.error("Error while connecting to MQTT server {} for {}", mqttConfig.serverURI(), name(), ex);

            recordSink.tryEmitError(ex);
        }

        return recordSink.asFlux();
    }

    /**
     * Close any open connections and free resources used by this class. Also emit a complete signal on the
     * {@code recordSink} of this datasource.
     */
    @Override
    public void close() {
        logger.info("Closing {}", name());

        if (asyncClient != null) {
            try {
                if (asyncClient.isConnected()) {
                    asyncClient.disconnect(DISCONNECT_TIMEOUT.toMillis());
                }
                asyncClient.close();
            } catch (MqttException ex) {
                logger.warn("Error while disconnecting or closing MQTT client", ex);
            }
        }

        recordSink.tryEmitComplete();
    }

    @Override
    public void disconnected(MqttDisconnectResponse disconnectResponse) {
        logger.warn("Disconnected from MQTT broker", disconnectResponse.getException());
    }

    @Override
    public void mqttErrorOccurred(MqttException exception) {
        logger.error("MQTT error occurred", exception);
    }

    /**
     * Called when the connection to the broker has been established and will then subscribe to the topic specified in
     * {@code mqttConfig}.
     *
     * @param reconnect If true, the connection was the result of automatic reconnect.
     * @param serverURI The server URI that the connection was made to.
     */
    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        logger.info("{} connected successfully to broker {}, was from automatic reconnect is {}",
                    name(),
                    serverURI,
                    reconnect);
        logger.info("Will subscribe to topic {}", mqttConfig.subscribeTopic());

        try {
            if (asyncClient != null) {
                asyncClient.subscribe(mqttConfig.subscribeTopic(), 2);
                subscribeToHealthTopic();
            }
        } catch (MqttException ex) {
            logger.error("Error while subscribing to topic {}", mqttConfig.subscribeTopic(), ex);
            recordSink.tryEmitError(ex);
        }
    }

    @Override
    public void authPacketArrived(int reasonCode, MqttProperties properties) {
        // implementation not needed by this datasource
    }

    @Override
    public Health health() {
        if (asyncClient == null) {
            return Health.down().withDetail("MqttDataSource", "Client is null.").build();
        }

        return (asyncClient.isConnected() ? Health.up() : Health.down()
                                                                .withDetail("MqttDataSource",
                                                                            "Client not connected with server " + asyncClient.getServerURI())).build();
    }

    protected void subscribeToHealthTopic() {
        // Not needed in MqttDataSource
    }

    private MqttConnectionOptions createConnectOptions() {
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setCleanStart(mqttConfig.cleanStart());
        options.setAutomaticReconnect(mqttConfig.automaticReconnect());
        options.setKeepAliveInterval(mqttConfig.keepAliveInterval());

        if (mqttConfig.username() != null) {
            options.setUserName(mqttConfig.username());
        }

        // extra variable required to avoid NPE warning
        String password = mqttConfig.password();
        if (password != null) {
            options.setPassword(password.getBytes(StandardCharsets.UTF_8));
        }

        return options;
    }
}