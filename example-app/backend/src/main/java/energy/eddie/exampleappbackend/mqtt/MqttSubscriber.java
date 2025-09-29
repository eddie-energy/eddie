package energy.eddie.exampleappbackend.mqtt;

import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import energy.eddie.exampleappbackend.serialization.DeserializationException;
import energy.eddie.exampleappbackend.serialization.MessageSerde;
import energy.eddie.exampleappbackend.serialization.SerdeFactory;
import energy.eddie.exampleappbackend.serialization.SerdeInitializationException;
import energy.eddie.exampleappbackend.service.RealTimeDataService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MqttSubscriber implements MqttCallback {
    private final RealTimeDataService realTimeDataService;
    private final MessageSerde messageSerde = SerdeFactory.getInstance().create("json"); // AIIDA currently only supports json

    public MqttSubscriber(RealTimeDataService realTimeDataService) throws SerdeInitializationException {
        this.realTimeDataService = realTimeDataService;
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
        // No advanced security required
    }

    @Override
    public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {
        log.warn("MQTT Connection was Disconnected!");
    }

}
