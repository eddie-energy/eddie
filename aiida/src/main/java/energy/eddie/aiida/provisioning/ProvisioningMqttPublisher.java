// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.provisioning;

import energy.eddie.aiida.models.mqtt.MqttConnection;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.utils.MqttFactory;
import jakarta.annotation.Nullable;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

public class ProvisioningMqttPublisher implements MqttCallback, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningMqttPublisher.class);
    private static final int DEFAULT_KEEP_ALIVE_INTERVAL = 60;
    private static final Duration DISCONNECT_TIMEOUT = Duration.ofSeconds(30);

    private final MqttConnection mqttConnection;
    private final String connectionUsername;
    private final String connectionPassword;
    private final String topic;
    @Nullable
    private IMqttAsyncClient client;

    /**
     * Creates a publisher and starts an asynchronous connection to the configured MQTT broker.
     *
     * @param mqttConnection MQTT connection and credentials used by the publisher.
     * @param topic          Topic to which inbound records are published.
     */
    public ProvisioningMqttPublisher(MqttConnection mqttConnection, String topic) {
        this(mqttConnection, topic, mqttConnection.username(), mqttConnection.password());
    }

    /**
     * Creates a publisher with connection credentials that are independent of the persisted provisioning user.
     * Server mode uses this overload so AIIDA connects with its stable broker identity while the generated
     * per-permission user remains dedicated to the provisioning client.
     *
     * @param mqttConnection     MQTT connection and persisted username.
     * @param topic              Topic to which inbound records are published.
     * @param connectionUsername Username used to authenticate this publisher.
     * @param connectionPassword Plaintext password used to authenticate this publisher.
     */
    public ProvisioningMqttPublisher(
            MqttConnection mqttConnection,
            String topic,
            String connectionUsername,
            String connectionPassword
    ) {
        this.mqttConnection = mqttConnection;
        this.topic = topic;
        this.connectionUsername = connectionUsername;
        this.connectionPassword = connectionPassword;

        connect();
    }

    /**
     * Publishes the payload of an inbound record when the MQTT client is connected. If the client is disconnected or
     * publishing fails, the record is not sent and the method returns without propagating an MQTT exception.
     *
     * @param inboundRecord Record whose payload should be published.
     */
    public void publish(InboundRecord inboundRecord) {
        var message = new MqttMessage(inboundRecord.payload().getBytes(StandardCharsets.UTF_8));
        try {
            if (client == null || !client.isConnected()) {
                return;
            }

            client.publish(topic, message);
        } catch (MqttException ex) {
            LOGGER.error("Error while publishing message to {} onto topic {}",
                         mqttConnection.internalHost(),
                         topic,
                         ex);
        }
    }

    /**
     * Disconnects and closes the MQTT client. MQTT errors encountered during shutdown are logged and not propagated.
     */
    @Override
    public void close() {
        if (client != null) {
            var clientId = client.getClientId();
            LOGGER.info("Closing client {}", clientId);
            try {
                if (client.isConnected()) {
                    client.disconnect(DISCONNECT_TIMEOUT.toMillis());
                }
                client.close();
            } catch (MqttException ex) {
                LOGGER.warn("Error while disconnecting or closing MQTT client {}", clientId, ex);
            }
        }
    }

    /**
     * Indicates whether the publisher currently has an established MQTT connection.
     *
     * @return {@code true} when an MQTT client exists and is connected; otherwise {@code false}.
     */
    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disconnected(MqttDisconnectResponse disconnectResponse) {
        LOGGER.warn("Disconnected from MQTT broker", disconnectResponse.getException());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mqttErrorOccurred(MqttException exception) {
        LOGGER.error("MQTT error occurred", exception);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        // This client only publishes to the given topic.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deliveryComplete(IMqttToken token) {
        LOGGER.info("Delivery complete for topic {}", topic);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        LOGGER.info("{} connected successfully to broker {}, automatic reconnect is {}",
                    mqttConnection.internalHost(),
                    serverURI,
                    reconnect);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void authPacketArrived(int reasonCode, MqttProperties properties) {
        // AuthPacket doesn't need to be processed here
    }

    private void connect() {
        try {
            var clientId = UUID.randomUUID();
            client = MqttFactory.getMqttAsyncClient(mqttConnection.internalHost(), clientId.toString(), null);
            client.setCallback(this);
            MqttConnectionOptions connectOptions = createConnectOptions();

            LOGGER.info("Connecting to broker {} with username {}",
                        mqttConnection.internalHost(),
                        connectOptions.getUserName());

            client.connect(connectOptions);
        } catch (MqttException ex) {
            LOGGER.error("Error while connecting to MQTT server {} for inbound provisioning",
                         mqttConnection.internalHost(),
                         ex);
        }
    }

    private MqttConnectionOptions createConnectOptions() {
        var options = new MqttConnectionOptions();

        options.setCleanStart(false);
        options.setAutomaticReconnect(true);
        options.setKeepAliveInterval(DEFAULT_KEEP_ALIVE_INTERVAL);
        options.setUserName(connectionUsername);
        options.setPassword(connectionPassword.getBytes(StandardCharsets.UTF_8));

        return options;
    }
}
