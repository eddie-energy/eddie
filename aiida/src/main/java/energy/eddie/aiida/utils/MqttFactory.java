package energy.eddie.aiida.utils;

import jakarta.annotation.Nullable;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttClientPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;

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
