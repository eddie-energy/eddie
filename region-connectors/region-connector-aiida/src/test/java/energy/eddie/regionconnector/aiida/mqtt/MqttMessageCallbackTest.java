package energy.eddie.regionconnector.aiida.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import energy.eddie.api.v0.ConnectionStatusMessage;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MqttMessageCallbackTest {
    private final TestLogger logger = TestLoggerFactory.getTestLogger(MqttMessageCallback.class);
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
        assertTrue(logger.isWarnEnabled());
        assertTrue(logger.getLoggingEvents()
                         .stream()
                         .anyMatch(
                                 loggingEvent ->
                                         loggingEvent.getMessage().equals("Disconnected from MQTT broker {}")
                                         && loggingEvent.getArguments().contains(disconnectResponse)
                         )
        );
    }

    @Test
    void testMqttErrorOccured() {
        // Given
        var mqttException = mock(MqttException.class);

        // When
        mqttMessageCallback.mqttErrorOccurred(mqttException);

        // Then
        assertTrue(logger.isErrorEnabled());
        assertTrue(logger.getLoggingEvents()
                         .stream()
                         .anyMatch(
                                 loggingEvent ->
                                         loggingEvent.getMessage().equals("Mqtt error occurred")
                         )
        );
    }

    @Test
    void testDeliveryComplete() {
        // Given
        var mqttToken = mock(IMqttToken.class);

        // When
        mqttMessageCallback.deliveryComplete(mqttToken);

        // Then
        assertTrue(logger.isTraceEnabled());
        assertTrue(logger.getLoggingEvents()
                         .stream()
                         .anyMatch(
                                 loggingEvent ->
                                         loggingEvent.getMessage().equals("Delivery complete for MqttToken {}")
                                         && loggingEvent.getArguments().contains(mqttToken)
                         )
        );
    }

    @Test
    void testConnectComplete() {
        // Given
        boolean reconnect = true;
        String serverURI = "tcp://test.com";

        // When
        mqttMessageCallback.connectComplete(reconnect, serverURI);

        // Then
        assertTrue(logger.isInfoEnabled());
        assertTrue(logger.getLoggingEvents()
                         .stream()
                         .anyMatch(
                                 loggingEvent ->
                                         loggingEvent.getMessage()
                                                     .equals("Connected to MQTT broker {}, was because of reconnect: {}")
                                         && loggingEvent.getArguments().contains(serverURI)
                                         && loggingEvent.getArguments().contains(reconnect)
                         )
        );
    }
}
