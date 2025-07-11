package energy.eddie.exampleappbackend.mqtt;

import energy.eddie.exampleappbackend.config.ExampleAppMqttConfig;
import energy.eddie.exampleappbackend.persistence.PermissionRepository;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
@AllArgsConstructor
public class MqttSubscriber implements MqttCallback {
    private static final Pattern PERMISSION_TOPIC_PATTERN = Pattern.compile("^aiida/v1/([\\w-]+)/data$");
    private MqttConnectionOptions mqttConnectionOptions;
    private ExampleAppMqttConfig exampleAppMqttConfig;
    private PermissionRepository permissionRepository;

    @PostConstruct
    public void init() {
        try {
            var client = new MqttClient(exampleAppMqttConfig.serverUri(), exampleAppMqttConfig.clientId());
            client.setCallback(this);
            client.connect(mqttConnectionOptions);
            client.subscribe("aiida/v1/+/data", 0);
            log.info("Connected to MQTT broker and subscribed to 'aiida/v1/+/data'!");

        } catch (MqttException e) {
            log.error("Failed to initialize MQTT subscriber", e);
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        var payload = new String(message.getPayload());
        log.info(payload);

        if (message.getPayload() == null) {
            log.warn("Received empty MQTT message on topic: {}", topic);
            return;
        }

        Matcher matcher = PERMISSION_TOPIC_PATTERN.matcher(topic);
        if (!matcher.matches()) {
            log.warn("Received message in MQTT topic, which does not match AIIDA topics!");
            return;
        }

        var eddiePermissionId = matcher.group(1);
        permissionRepository.findByEddiePermissionId(eddiePermissionId).ifPresentOrElse((permission) -> {
            if (permission.getTimeSeriesList() == null) {

            } else {

            }
        }, () -> {
            log.debug("Received message for permission with EDDIE permissionId: {}, which is not registered! Ignoring message", eddiePermissionId);
        });
    }

    @Override
    public void mqttErrorOccurred(MqttException exception) {
        log.warn("MQTT Exception occurred: {}", exception.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttToken token) {
        log.debug("Delivered mqtt token: {}", token);
    }

    @Override
    public void connectComplete(boolean reconnect, String serverUri) {
        log.info("Successfully connected to MQTT broker with uri: {}! Connection was triggered by reconnect: {}", serverUri, reconnect);
    }

    @Override
    public void authPacketArrived(int i, MqttProperties mqttProperties) {
    }

    @Override
    public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {
        log.warn("MQTT Connection was Disconnected!");
    }

}
