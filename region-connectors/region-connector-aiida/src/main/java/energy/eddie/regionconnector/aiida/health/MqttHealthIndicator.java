package energy.eddie.regionconnector.aiida.health;

import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class MqttHealthIndicator implements HealthIndicator {
    private final MqttAsyncClient mqttClient;

    public MqttHealthIndicator(MqttAsyncClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    @Override
    public Health health() {
        return (mqttClient.isConnected() ? Health.up() : Health.down()).build();
    }
}
