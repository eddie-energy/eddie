// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.mqtt;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Duration;
import java.time.Instant;

public class MqttConnectCallback implements MqttActionListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttConnectCallback.class);
    private static final Duration reconnectDelay = Duration.ofSeconds(15);
    private final MqttAsyncClient client;
    private final MqttConnectionOptions connectionOptions;
    private final ThreadPoolTaskScheduler scheduler;

    /**
     * Creates a new MqttConnectCallback that will retry the connection after 15s delay if the
     * {@link MqttConnectCallback#onFailure(IMqttToken, Throwable)} listener is called by the {@code client}.
     */
    public MqttConnectCallback(
            MqttAsyncClient client,
            MqttConnectionOptions connectionOptions,
            ThreadPoolTaskScheduler scheduler
    ) {
        this.client = client;
        this.connectionOptions = connectionOptions;
        this.scheduler = scheduler;
    }

    @Override
    public void onSuccess(IMqttToken asyncActionToken) {
        LOGGER.atInfo()
              .addArgument(asyncActionToken.getClient().getServerURI())
              .log("Successfully connected to MQTT broker {}");
    }

    @Override
    @SuppressWarnings("FutureReturnValueIgnored")
    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        LOGGER.atError()
              .addArgument(asyncActionToken.getClient().getServerURI())
              .addArgument(reconnectDelay.toSeconds())
              .log("Failed to connect to MQTT broker {}, will retry in {} seconds");

        scheduler.schedule(() -> {
            try {
                client.connect(connectionOptions, null, this);
            } catch (MqttException e) {
                LOGGER.atError()
                      .setCause(e)
                      .log("Failed to connect to MQTT broker");
            }
        }, Instant.now().plus(reconnectDelay));
    }
}
