package energy.eddie.regionconnector.aiida.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.v0.ConnectionStatusMessage;
import nl.altindag.log.LogCaptor;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MqttMessageCallbackTest {
    private final LogCaptor logCaptor = LogCaptor.forClass(MqttMessageCallback.class);
    private final ObjectMapper realObjectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module());
    private Sinks.Many<String> revocationSink;
    private ObjectMapper objectMapper;
    private MqttMessageCallback mqttMessageCallback;

    @BeforeEach
    void setUp() {
        revocationSink = mock(Sinks.Many.class);
        objectMapper = mock(ObjectMapper.class);
        mqttMessageCallback = new MqttMessageCallback(revocationSink, objectMapper);
    }

    @AfterEach
    void tearDown() {
        logCaptor.clearLogs();
        logCaptor.resetLogLevel();
    }

    @Test
    void testMessageArrived_StatusMessageRevoked() throws IOException {
        // Given
        String topic = "test/status";
        String permissionId = "test";
        String payload = "{\"connectionId\":\"30\",\"dataNeedId\":\"test\",\"timestamp\":1725458241.237425343,\"status\":\"REVOKED\",\"permissionId\":\"test\"}";

        MqttMessage mqttMessage = new MqttMessage(payload.getBytes());
        ConnectionStatusMessage connectionStatusMessage = realObjectMapper.readValue(payload.getBytes(),
                                                                                     ConnectionStatusMessage.class);

        when(objectMapper.readValue(anyString(),
                                    eq(ConnectionStatusMessage.class))).thenReturn(connectionStatusMessage);
        Sinks.EmitResult successResult = Sinks.EmitResult.OK;
        when(revocationSink.tryEmitNext(permissionId)).thenReturn(successResult);

        // When
        mqttMessageCallback.messageArrived(topic, mqttMessage);

        // Then
        verify(objectMapper).readValue(mqttMessage.toString(), ConnectionStatusMessage.class);
        verify(revocationSink).tryEmitNext(permissionId);
    }

    @Test
    void testMessageArrived_StatusMessageNotRevoked() throws IOException {
        // Given
        String topic = "some/topic/status";
        String payload = "{\"connectionId\":\"30\",\"dataNeedId\":\"test\",\"timestamp\":1725458241.237425343,\"status\":\"ACCEPTED\",\"permissionId\":\"test\"}";

        MqttMessage mqttMessage = new MqttMessage(payload.getBytes());
        ConnectionStatusMessage connectionStatusMessage = realObjectMapper.readValue(payload.getBytes(),
                                                                                     ConnectionStatusMessage.class);

        when(objectMapper.readValue(anyString(),
                                    eq(ConnectionStatusMessage.class))).thenReturn(connectionStatusMessage);

        // When
        mqttMessageCallback.messageArrived(topic, mqttMessage);

        // Then
        verify(revocationSink, never()).tryEmitNext(anyString());
    }

    @Test
    void testDisconnected() {
        // Given
        var disconnectResponse = mock(MqttDisconnectResponse.class);

        // When
        mqttMessageCallback.disconnected(disconnectResponse);

        // Then
        assertTrue(logCaptor.getWarnLogs().stream().anyMatch(log -> log.startsWith("Disconnected from MQTT broker")));
    }

    @Test
    void testMqttErrorOccured() {
        // Given
        var mqttException = mock(MqttException.class);

        // When
        mqttMessageCallback.mqttErrorOccurred(mqttException);

        // Then
        assertTrue(logCaptor.getErrorLogs().contains("Mqtt error occurred"));
    }

    @Test
    void testDeliveryComplete() {
        // Given
        var mqttToken = mock(IMqttToken.class);
        logCaptor.setLogLevelToTrace();

        // When
        mqttMessageCallback.deliveryComplete(mqttToken);

        // Then
        assertTrue(logCaptor.getTraceLogs()
                            .stream()
                            .anyMatch(log -> log.startsWith("Delivery complete for MqttToken")));
    }

    @Test
    void testConnectComplete() {
        // Given
        boolean reconnect = true;
        String serverURI = "tcp://test.com";

        // When
        mqttMessageCallback.connectComplete(reconnect, serverURI);

        // Then
        assertTrue(logCaptor.getInfoLogs().stream().anyMatch(log -> log.startsWith("Connected to MQTT broker")));
    }
}
