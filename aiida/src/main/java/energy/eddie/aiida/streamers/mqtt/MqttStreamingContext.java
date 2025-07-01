package energy.eddie.aiida.streamers.mqtt;

import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;

public record MqttStreamingContext(MqttAsyncClient client, MqttStreamingConfig streamingConfig) {
}
