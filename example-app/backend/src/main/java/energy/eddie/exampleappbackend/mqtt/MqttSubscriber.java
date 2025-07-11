package energy.eddie.exampleappbackend.mqtt;

import energy.eddie.cim.v1_04.RTDEnvelope;
import energy.eddie.exampleappbackend.config.ExampleAppMqttConfig;
import energy.eddie.exampleappbackend.serialization.DeserializationException;
import energy.eddie.exampleappbackend.serialization.MessageSerde;
import energy.eddie.exampleappbackend.serialization.SerdeFactory;
import energy.eddie.exampleappbackend.serialization.SerdeInitializationException;
import energy.eddie.exampleappbackend.service.RealTimeDataService;
import jakarta.annotation.PostConstruct;
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

import java.util.regex.Pattern;

@Component
@Slf4j
public class MqttSubscriber implements MqttCallback {
    private static final Pattern PERMISSION_TOPIC_PATTERN = Pattern.compile("^aiida/v1/([\\w-]+)/data$");
    private final MqttConnectionOptions mqttConnectionOptions;
    private final ExampleAppMqttConfig exampleAppMqttConfig;
    private final RealTimeDataService realTimeDataService;
    private final MessageSerde messageSerde = SerdeFactory.getInstance().create("json");

    public MqttSubscriber(MqttConnectionOptions mqttConnectionOptions, ExampleAppMqttConfig exampleAppMqttConfig, RealTimeDataService realTimeDataService) throws SerdeInitializationException {
        this.mqttConnectionOptions = mqttConnectionOptions;
        this.exampleAppMqttConfig = exampleAppMqttConfig;
        this.realTimeDataService = realTimeDataService;
    }


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
        if (message.getPayload() == null) {
            log.warn("Received empty MQTT message on topic: {}", topic);
            return;
        }

        try {
            var rtdEnvelope = messageSerde.deserialize(message.getPayload(), RTDEnvelope.class);
            realTimeDataService.handelRealTimeDataEnvelope(rtdEnvelope);
        } catch (DeserializationException e) {
            log.info("Failed to deserialize RTD envelope from message on topic {}! Ignoring Message -- could have other format than XML!", topic);
        }
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
