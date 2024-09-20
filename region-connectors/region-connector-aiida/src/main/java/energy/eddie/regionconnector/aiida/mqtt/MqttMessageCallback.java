package energy.eddie.regionconnector.aiida.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Sinks;

import java.io.IOException;

public class MqttMessageCallback implements MqttCallback {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttMessageCallback.class);
    private final Sinks.Many<String> revocationSink;
    private final ObjectMapper objectMapper;

    public MqttMessageCallback(Sinks.Many<String> revocationSink, ObjectMapper objectMapper) {
        this.revocationSink = revocationSink;
        this.objectMapper = objectMapper;
    }

    @Override
    public void disconnected(MqttDisconnectResponse disconnectResponse) {
        LOGGER.warn("Disconnected from MQTT broker {}", disconnectResponse);
    }

    @Override
    public void mqttErrorOccurred(MqttException exception) {
        LOGGER.error("Mqtt error occurred", exception);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws IOException {
        if (topic.endsWith(TopicType.STATUS.topicName())) {
            var statusMessage = objectMapper.readValue(message.toString(), ConnectionStatusMessage.class);

            if (statusMessage.status().equals(PermissionProcessStatus.REVOKED)) {
                LOGGER.info("Received connection status message to revoke permission {}", statusMessage.permissionId());
                revocationSink.tryEmitNext(statusMessage.permissionId());
            }
        }
    }

    @Override
    public void deliveryComplete(IMqttToken token) {
        LOGGER.trace("Delivery complete for MqttToken {}", token);
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        LOGGER.info("Connected to MQTT broker {}, was because of reconnect: {}", serverURI, reconnect);
    }

    @Override
    public void authPacketArrived(int reasonCode, MqttProperties properties) {
        // Not needed, as no advanced authentication is required
    }
}
