package energy.eddie.regionconnector.aiida.mqtt.message.processor;

import energy.eddie.regionconnector.aiida.exceptions.PermissionInvalidException;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.eclipse.paho.mqttv5.common.MqttMessage;

import java.io.IOException;

public interface AiidaMessageProcessor {
    void processMessage(MqttMessage message) throws IOException, PermissionNotFoundException, PermissionInvalidException;

    String forTopicPath();
}
