// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.inbound;

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

public class InboundDataMqttClient implements MqttCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(InboundDataMqttClient.class);
    private static final int DEFAULT_KEEP_ALIVE_INTERVAL = 60;
    private static final Duration DISCONNECT_TIMEOUT = Duration.ofSeconds(30);

    private final MqttConnection mqttConnection;
    private final String topic;
    @Nullable
    private IMqttAsyncClient client;

    public InboundDataMqttClient(MqttConnection mqttConnection, String topic) {
        this.mqttConnection = mqttConnection;
        this.topic = topic;

        connect();
    }

    public void publish(InboundRecord inboundRecord) {
        var message = new MqttMessage(inboundRecord.payload().getBytes(StandardCharsets.UTF_8));
        try {
            if (client == null || client.isConnected()) {
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

    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    @Override
    public void disconnected(MqttDisconnectResponse disconnectResponse) {
        LOGGER.warn("Disconnected from MQTT broker", disconnectResponse.getException());
    }

    @Override
    public void mqttErrorOccurred(MqttException exception) {
        LOGGER.error("MQTT error occurred", exception);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        // Messages don't need to be processed here
    }

    @Override
    public void deliveryComplete(IMqttToken token) {
        LOGGER.info("Delivery complete for topic {}", topic);
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        LOGGER.info("{} connected successfully to broker {}, automatic reconnect is {}",
                    mqttConnection.internalHost(),
                    serverURI,
                    reconnect);
        LOGGER.info("Will subscribe to topic {}", topic);

        try {
            if (client != null) {
                client.subscribe(topic, 2);
            }
        } catch (MqttException ex) {
            LOGGER.error("Error while subscribing to topic {}", topic, ex);
        }
    }

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
        MqttConnectionOptions options = new MqttConnectionOptions();

        options.setCleanStart(false);
        options.setAutomaticReconnect(true);
        options.setKeepAliveInterval(DEFAULT_KEEP_ALIVE_INTERVAL);
        options.setUserName(mqttConnection.username());
        options.setPassword(mqttConnection.password().getBytes(StandardCharsets.UTF_8));

        return options;
    }
}
