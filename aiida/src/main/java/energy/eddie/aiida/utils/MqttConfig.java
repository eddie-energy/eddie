package energy.eddie.aiida.utils;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

/**
 * Contains connection details to subscribe to a MQTT broker.
 * May at a later stage also contain authentication credentials.
 *
 * @param serverURI          URI of the server to connect to.
 * @param subscribeTopic     Topic to which should be subscribed.
 * @param cleanSession       If true, server and client will not retain state across client restarts.
 * @param automaticReconnect If true, the MQTT client will automatically try to reconnect to the server if connection is lost.
 * @param keepAliveInterval  Duration in seconds for the keepAliveInterval, see {@link MqttConnectOptions#setKeepAliveInterval(int)}
 */
public record MqttConfig(
        String serverURI,
        String subscribeTopic,
        Boolean cleanSession,
        Boolean automaticReconnect,
        Integer keepAliveInterval) {
    public MqttConfig(String serverURI, String subscribeTopic) {
        this(serverURI, subscribeTopic, false, true, 60);
    }

    public MqttConfig(String serverURI, String subscribeTopic, Integer keepAliveInterval) {
        this(serverURI, subscribeTopic, false, true, keepAliveInterval);
    }

    public MqttConfig {
        if (keepAliveInterval < 0)
            throw new IllegalArgumentException("keepAliveInterval needs to be <= 0 seconds");
    }
}
