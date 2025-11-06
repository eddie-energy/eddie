package energy.eddie.regionconnector.aiida.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.agnostic.aiida.AiidaConnectionStatusMessageDto;
import energy.eddie.regionconnector.aiida.exceptions.MqttTopicException;
import energy.eddie.regionconnector.aiida.exceptions.PermissionInvalidException;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import nl.altindag.log.LogCaptor;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MqttMessageCallbackTest {
    private final LogCaptor logCaptor = LogCaptor.forClass(MqttMessageCallback.class);
    private final ObjectMapper realObjectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module());
    @Mock
    private Sinks.Many<AiidaConnectionStatusMessageDto> statusSink;
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private MqttMessageCallback mqttMessageCallback;

    @AfterEach
    void tearDown() {
        logCaptor.clearLogs();
        logCaptor.resetLogLevel();
    }

    @Test
    void testMessageArrived_StatusMessageRevoked() throws IOException, MqttTopicException, PermissionInvalidException, PermissionNotFoundException {
        // Given
        String topic = "test/status";
        String payload = "{\"connectionId\":\"30\",\"dataNeedId\":\"00000000-0000-0000-0000-000000000000\",\"timestamp\":1725458241.237425343,\"status\":\"REVOKED\",\"permissionId\":\"00000000-0000-0000-0000-000000000001\",\"eddieId\":\"00000000-0000-0000-0000-000000000002\"}";

        MqttMessage mqttMessage = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        AiidaConnectionStatusMessageDto connectionStatusMessage = realObjectMapper.readValue(payload.getBytes(StandardCharsets.UTF_8),
                                                                                             AiidaConnectionStatusMessageDto.class);

        when(objectMapper.readValue(anyString(),
                                    eq(AiidaConnectionStatusMessageDto.class))).thenReturn(connectionStatusMessage);
        Sinks.EmitResult successResult = Sinks.EmitResult.OK;
        when(statusSink.tryEmitNext(connectionStatusMessage)).thenReturn(successResult);

        // When
        mqttMessageCallback.messageArrived(topic, mqttMessage);

        // Then
        verify(objectMapper).readValue(mqttMessage.toString(), AiidaConnectionStatusMessageDto.class);
        verify(statusSink).tryEmitNext(connectionStatusMessage);
    }

    @Test
    void testMessageArrived_StatusMessageNotRevoked() throws IOException, MqttTopicException, PermissionInvalidException, PermissionNotFoundException {
        // Given
        String topic = "some/topic/status";
        String payload = "{\"connectionId\":\"30\",\"dataNeedId\":\"00000000-0000-0000-0000-000000000000\",\"timestamp\":1725458241.237425343,\"status\":\"ACCEPTED\",\"permissionId\":\"00000000-0000-0000-0000-000000000001\",\"eddieId\":\"00000000-0000-0000-0000-000000000002\"}";

        MqttMessage mqttMessage = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        AiidaConnectionStatusMessageDto connectionStatusMessage = realObjectMapper.readValue(payload.getBytes(StandardCharsets.UTF_8),
                                                                                             AiidaConnectionStatusMessageDto.class);

        when(objectMapper.readValue(anyString(),
                                    eq(AiidaConnectionStatusMessageDto.class))).thenReturn(connectionStatusMessage);

        // When
        mqttMessageCallback.messageArrived(topic, mqttMessage);

        // Then
        verify(statusSink).tryEmitNext(connectionStatusMessage);
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
