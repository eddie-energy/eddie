package energy.eddie.aiida.utils;

import jakarta.annotation.Nullable;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttException;

public class MqttFactory {
    private MqttFactory() {
    }

    public static MqttAsyncClient getMqttAsyncClient(
            String serverURI,
            String clientId,
            @Nullable MqttClientPersistence persistence) throws MqttException {
        return new MqttAsyncClient(serverURI, clientId, persistence);
    }
}
