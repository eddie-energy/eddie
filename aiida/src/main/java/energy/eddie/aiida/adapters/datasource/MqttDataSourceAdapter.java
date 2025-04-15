package energy.eddie.aiida.adapters.datasource;

import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
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

public abstract class MqttDataSourceAdapter<T extends MqttDataSource> extends DataSourceAdapter<T> implements MqttCallback {
    private static final Integer DEFAULT_KEEP_ALIVE_INTERVAL = 60;
    private static final Duration DISCONNECT_TIMEOUT = Duration.ofSeconds(30);
    private final Logger logger;
    @Nullable
    protected MqttAsyncClient asyncClient;
    private Integer keepAliveInterval = DEFAULT_KEEP_ALIVE_INTERVAL;

    /**
     * Creates a new {@code MqttDataSourceAdapter} with the specified display name and mqttConfig.
     *
     * @param dataSource The entity of the data source.
     */
    protected MqttDataSourceAdapter(T dataSource, Logger childLogger) {
        super(dataSource);
        logger = childLogger;
    }

    /**
     * Start the listening or active polling for data from the datasource. The {@code time} field from the adapter is
     * interpreted as unix timestamp.
     */
    @Override
    public Flux<AiidaRecord> start() {
        logger.info("Starting {}", dataSource().name());

        try {
            // persistence is only required when publishing messages with QOS 1 or 2
            var clientId = UUID.randomUUID();
            asyncClient = MqttFactory.getMqttAsyncClient(dataSource().mqttInternalHost(), clientId.toString(), null);
            asyncClient.setCallback(this);

            MqttConnectionOptions connectOptions = createConnectOptions();

            logger.info("Connecting to broker {}", dataSource().mqttInternalHost());

            asyncClient.connect(connectOptions);
        } catch (MqttException ex) {
            logger.error("Error while connecting to MQTT server {} for {}",
                         dataSource().mqttPassword(),
                         dataSource().name(),
                         ex);

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
        logger.info("Closing {}", dataSource().name());

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
                    dataSource().name(),
                    serverURI,
                    reconnect);
        logger.info("Will subscribe to topic {}", dataSource().mqttSubscribeTopic());

        try {
            if (asyncClient != null) {
                asyncClient.subscribe(dataSource().mqttSubscribeTopic(), 2);
                subscribeToHealthTopic();
            }
        } catch (MqttException ex) {
            logger.error("Error while subscribing to topic {}", dataSource().mqttSubscribeTopic(), ex);
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

        return (asyncClient.isConnected()
                ? Health.up()
                : Health.down().withDetail("MqttDataSource",
                                           "Client not connected with server " + asyncClient.getServerURI())).build();
    }

    public void setKeepAliveInterval(Integer keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval;
    }

    protected void subscribeToHealthTopic() {
        // Not needed in MqttDataSource
    }

    private MqttConnectionOptions createConnectOptions() {
        MqttConnectionOptions options = new MqttConnectionOptions();

        options.setCleanStart(false);
        options.setAutomaticReconnect(true);
        options.setKeepAliveInterval(keepAliveInterval);
        options.setUserName(dataSource().mqttUsername());
        options.setPassword(dataSource().mqttPassword().getBytes(StandardCharsets.UTF_8));

        return options;
    }
}