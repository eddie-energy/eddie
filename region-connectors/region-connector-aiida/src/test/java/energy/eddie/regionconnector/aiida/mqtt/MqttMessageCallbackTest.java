package energy.eddie.regionconnector.aiida.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.aiida.AiidaConnectionStatusMessageDto;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import energy.eddie.regionconnector.aiida.exceptions.MqttTopicException;
import energy.eddie.regionconnector.aiida.exceptions.PermissionInvalidException;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionRequestViewRepository;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import nl.altindag.log.LogCaptor;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqttMessageCallbackTest {
    private final LogCaptor logCaptor = LogCaptor.forClass(MqttMessageCallback.class);
    private final ObjectMapper realObjectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module());
    private final Sinks.Many<AiidaConnectionStatusMessageDto> statusSink = Sinks.many()
                                                                                .unicast()
                                                                                .onBackpressureBuffer();
    private final Sinks.Many<RTDEnvelope> nearRealTimeDataSink = Sinks.many().unicast().onBackpressureBuffer();
    private final Sinks.Many<RawDataMessage> rawDataMessageSink = Sinks.many().unicast().onBackpressureBuffer();

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private AiidaPermissionRequestViewRepository permissionRequestViewRepository;

    private MqttMessageCallback mqttMessageCallback;

    @BeforeEach
    void setUp() {
        mqttMessageCallback = new MqttMessageCallback(
                permissionRequestViewRepository,
                statusSink,
                nearRealTimeDataSink,
                rawDataMessageSink,
                objectMapper
        );
    }

    @AfterEach
    void tearDown() {
        logCaptor.clearLogs();
        logCaptor.resetLogLevel();
    }

    @Test
    void messageArrived_statusMessage_revoked() throws Exception {
        // Given
        String topic = "aiida/v1/00000000-0000-0000-0000-000000000001/status";
        String payload = "{\"connectionId\":\"30\",\"dataNeedId\":\"00000000-0000-0000-0000-000000000000\",\"timestamp\":1725458241.237425343,\"status\":\"REVOKED\",\"permissionId\":\"00000000-0000-0000-0000-000000000001\",\"eddieId\":\"00000000-0000-0000-0000-000000000002\"}";

        MqttMessage mqttMessage = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        AiidaConnectionStatusMessageDto connectionStatusMessage = realObjectMapper.readValue(payload.getBytes(
                                                                                                     StandardCharsets.UTF_8),
                                                                                             AiidaConnectionStatusMessageDto.class);

        when(objectMapper.readValue(anyString(),
                                    eq(AiidaConnectionStatusMessageDto.class))).thenReturn(connectionStatusMessage);

        // When
        StepVerifier.create(statusSink.asFlux())
                    .then(() -> {
                        try {
                            mqttMessageCallback.messageArrived(topic, mqttMessage);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .assertNext(msg -> {
                        assertEquals(connectionStatusMessage, msg);
                        assertEquals(PermissionProcessStatus.REVOKED, msg.status());
                    })
                    .thenCancel()
                    .verify();
    }

    @Test
    void messageArrived_statusMessage_accepted() throws IOException {
        // Given
        String topic = "aiida/v1/00000000-0000-0000-0000-000000000001/status";
        String payload = "{\"connectionId\":\"30\",\"dataNeedId\":\"00000000-0000-0000-0000-000000000000\",\"timestamp\":1725458241.237425343,\"status\":\"ACCEPTED\",\"permissionId\":\"00000000-0000-0000-0000-000000000001\",\"eddieId\":\"00000000-0000-0000-0000-000000000002\"}";

        MqttMessage mqttMessage = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        AiidaConnectionStatusMessageDto connectionStatusMessage = realObjectMapper.readValue(payload.getBytes(
                                                                                                     StandardCharsets.UTF_8),
                                                                                             AiidaConnectionStatusMessageDto.class);

        when(objectMapper.readValue(anyString(),
                                    eq(AiidaConnectionStatusMessageDto.class))).thenReturn(connectionStatusMessage);

        // When
        StepVerifier.create(statusSink.asFlux())
                    .then(() -> {
                        try {
                            mqttMessageCallback.messageArrived(topic, mqttMessage);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .assertNext(msg -> {
                        assertEquals(connectionStatusMessage, msg);
                        assertEquals(PermissionProcessStatus.ACCEPTED, msg.status());
                    })
                    .thenCancel()
                    .verify();
    }

    @Test
    void messageArrived_smartMeterP1CimMessage_valid() throws IOException {
        // Given
        var topic = "aiida/v1/perm-1/data/outbound/smart-meter-p1-cim";

        var rtdEnvelope = new RTDEnvelope();
        rtdEnvelope.withMessageDocumentHeaderMetaInformationPermissionId("perm-1");
        when(objectMapper.readValue(anyString(), eq(RTDEnvelope.class))).thenReturn(rtdEnvelope);

        var permission = mock(AiidaPermissionRequest.class);
        when(permission.status()).thenReturn(PermissionProcessStatus.ACCEPTED);
        when(permission.start()).thenReturn(LocalDate.now(ZoneId.systemDefault()).minusDays(1));
        when(permission.end()).thenReturn(LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        when(permissionRequestViewRepository.findByPermissionId("perm-1")).thenReturn(Optional.of(permission));

        // When
        StepVerifier.create(nearRealTimeDataSink.asFlux())
                    .then(() -> {
                        try {
                            mqttMessageCallback.messageArrived(topic, new MqttMessage());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .assertNext(msg -> assertEquals(rtdEnvelope, msg))
                    .thenCancel()
                    .verify();

    }

    @Test
    void messageArrived_smartMeterP1CimMessage_invalidPermission() throws IOException {
        // Given
        var topic = "aiida/v1/perm-1/data/outbound/smart-meter-p1-cim";

        var rtdEnvelope = new RTDEnvelope();
        rtdEnvelope.withMessageDocumentHeaderMetaInformationPermissionId("perm-1");
        when(objectMapper.readValue(anyString(), eq(RTDEnvelope.class))).thenReturn(rtdEnvelope);

        when(permissionRequestViewRepository.findByPermissionId("perm-1")).thenReturn(Optional.empty());

        // When, Then
        assertThrows(PermissionNotFoundException.class, () ->
                mqttMessageCallback.messageArrived(topic, new MqttMessage())
        );
    }

    @Test
    void messageArrived_smartMeterP1CimMessage_invalidStatus() throws IOException {
        // Given
        var topic = "aiida/v1/perm-1/data/outbound/smart-meter-p1-cim";

        var rtdEnvelope = new RTDEnvelope();
        rtdEnvelope.withMessageDocumentHeaderMetaInformationPermissionId("perm-1");
        when(objectMapper.readValue(anyString(), eq(RTDEnvelope.class))).thenReturn(rtdEnvelope);

        var permission = mock(AiidaPermissionRequest.class);
        when(permission.status()).thenReturn(PermissionProcessStatus.REVOKED);
        when(permissionRequestViewRepository.findByPermissionId("perm-1")).thenReturn(Optional.of(permission));

        // When, Then
        assertThrows(PermissionInvalidException.class, () ->
                mqttMessageCallback.messageArrived(topic, new MqttMessage())
        );
    }

    @Test
    void messageArrived_smartMeterP1CimMessage_beforeStartDate() throws IOException {
        // Given
        var topic = "aiida/v1/perm-1/data/outbound/smart-meter-p1-cim";

        var rtdEnvelope = new RTDEnvelope();
        rtdEnvelope.withMessageDocumentHeaderMetaInformationPermissionId("perm-1");
        when(objectMapper.readValue(anyString(), eq(RTDEnvelope.class))).thenReturn(rtdEnvelope);

        var permission = mock(AiidaPermissionRequest.class);
        when(permission.status()).thenReturn(PermissionProcessStatus.ACCEPTED);
        when(permission.start()).thenReturn(LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        when(permission.end()).thenReturn(LocalDate.now(ZoneId.systemDefault()).plusDays(10));
        when(permissionRequestViewRepository.findByPermissionId("perm-1")).thenReturn(Optional.of(permission));

        // When, Then
        assertThrows(PermissionInvalidException.class, () ->
                mqttMessageCallback.messageArrived(topic, new MqttMessage())
        );
    }

    @Test
    void messageArrived_smartMeterP1CimMessage_afterEndDate() throws IOException {
        // Given
        var topic = "aiida/v1/perm-1/data/outbound/smart-meter-p1-cim";

        var rtdEnvelope = new RTDEnvelope();
        rtdEnvelope.withMessageDocumentHeaderMetaInformationPermissionId("perm-1");
        when(objectMapper.readValue(anyString(), eq(RTDEnvelope.class))).thenReturn(rtdEnvelope);

        var permission = mock(AiidaPermissionRequest.class);
        when(permission.status()).thenReturn(PermissionProcessStatus.ACCEPTED);
        when(permission.start()).thenReturn(LocalDate.now(ZoneId.systemDefault()).minusDays(10));
        when(permission.end()).thenReturn(LocalDate.now(ZoneId.systemDefault()).minusDays(1));
        when(permissionRequestViewRepository.findByPermissionId("perm-1")).thenReturn(Optional.of(permission));

        // When, Then
        assertThrows(PermissionInvalidException.class, () ->
                mqttMessageCallback.messageArrived(topic, new MqttMessage())
        );
    }

    @Test
    void messageArrived_smartMeterP1RawMessage_valid() {
        // Given
        var topic = "aiida/v1/perm-1/data/outbound/smart-meter-p1-raw";
        var payload = "{\"some\":\"data\"}";

        var permission = mock(AiidaPermissionRequest.class);
        when(permission.permissionId()).thenReturn("perm-1");
        when(permission.status()).thenReturn(PermissionProcessStatus.ACCEPTED);
        when(permission.start()).thenReturn(LocalDate.now(ZoneId.systemDefault()).minusDays(1));
        when(permission.end()).thenReturn(LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        when(permissionRequestViewRepository.findByPermissionId("perm-1")).thenReturn(Optional.of(permission));

        MqttMessage mqttMessage = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));

        // When
        StepVerifier.create(rawDataMessageSink.asFlux())
                    .then(() -> {
                        try {
                            mqttMessageCallback.messageArrived(topic, mqttMessage);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .assertNext(msg -> {
                        assertEquals("perm-1", msg.permissionId());
                        assertEquals(payload, msg.rawPayload());
                    })
                    .thenCancel()
                    .verify();
    }

    @Test
    void messageArrived_smartMeterP1RawMessage_invalidTopic() {
        // Given
        var topic = "invalid/v1/perm-1/data/outbound/smart-meter-p1-raw";
        var payload = "{\"some\":\"data\"}";

        MqttMessage mqttMessage = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));

        // When, Then
        assertThrows(MqttTopicException.class, () ->
                mqttMessageCallback.messageArrived(topic, mqttMessage)
        );
    }

    @Test
    void messageArrived_smartMeterP1RawMessage_invalidPermission() {
        // Given
        var topic = "aiida/v1/perm-1/data/outbound/smart-meter-p1-raw";
        var payload = "{\"some\":\"data\"}";

        when(permissionRequestViewRepository.findByPermissionId("perm-1")).thenReturn(Optional.empty());

        MqttMessage mqttMessage = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));

        // When, Then
        assertThrows(PermissionNotFoundException.class, () ->
                mqttMessageCallback.messageArrived(topic, mqttMessage)
        );
    }

    @Test
    void messageArrived_smartMeterP1RawMessage_invalidStatus() {
        // Given
        var topic = "aiida/v1/perm-1/data/outbound/smart-meter-p1-raw";
        var payload = "{\"some\":\"data\"}";

        var permission = mock(AiidaPermissionRequest.class);
        when(permission.status()).thenReturn(PermissionProcessStatus.REVOKED);
        when(permissionRequestViewRepository.findByPermissionId("perm-1")).thenReturn(Optional.of(permission));

        MqttMessage mqttMessage = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));

        // When, Then
        assertThrows(PermissionInvalidException.class, () ->
                mqttMessageCallback.messageArrived(topic, mqttMessage)
        );
    }

    @Test
    void messageArrived_smartMeterP1RawMessage_beforeStartDate() {
        // Given
        var topic = "aiida/v1/perm-1/data/outbound/smart-meter-p1-raw";
        var payload = "{\"some\":\"data\"}";

        var permission = mock(AiidaPermissionRequest.class);
        when(permission.status()).thenReturn(PermissionProcessStatus.ACCEPTED);
        when(permission.start()).thenReturn(LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        when(permission.end()).thenReturn(LocalDate.now(ZoneId.systemDefault()).plusDays(10));
        when(permissionRequestViewRepository.findByPermissionId("perm-1")).thenReturn(Optional.of(permission));

        MqttMessage mqttMessage = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));

        // When, Then
        assertThrows(PermissionInvalidException.class, () ->
                mqttMessageCallback.messageArrived(topic, mqttMessage)
        );
    }

    @Test
    void messageArrived_smartMeterP1RawMessage_afterEndDate() {
        // Given
        var topic = "aiida/v1/perm-1/data/outbound/smart-meter-p1-raw";
        var payload = "{\"some\":\"data\"}";

        var permission = mock(AiidaPermissionRequest.class);
        when(permission.status()).thenReturn(PermissionProcessStatus.ACCEPTED);
        when(permission.start()).thenReturn(LocalDate.now(ZoneId.systemDefault()).minusDays(10));
        when(permission.end()).thenReturn(LocalDate.now(ZoneId.systemDefault()).minusDays(1));
        when(permissionRequestViewRepository.findByPermissionId("perm-1")).thenReturn(Optional.of(permission));

        MqttMessage mqttMessage = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));

        // When, Then
        assertThrows(PermissionInvalidException.class, () ->
                mqttMessageCallback.messageArrived(topic, mqttMessage)
        );
    }

    @Test
    void messageArrived_unknownTopic()
            throws MqttTopicException, PermissionInvalidException, PermissionNotFoundException, JsonProcessingException {
        // Given
        var topic = "aiida/v1/test/unknown/topic";

        // When
        mqttMessageCallback.messageArrived(topic, new MqttMessage());

        // Then
        assertTrue(logCaptor.getWarnLogs().stream().anyMatch(log -> log.contains("unknown topic")));
    }

    @Test
    void disconnected() {
        // Given
        var disconnectResponse = mock(MqttDisconnectResponse.class);

        // When
        mqttMessageCallback.disconnected(disconnectResponse);

        // Then
        assertTrue(logCaptor.getWarnLogs().stream().anyMatch(log -> log.startsWith("Disconnected from MQTT broker")));
    }

    @Test
    void mqttErrorOccured() {
        // Given
        var mqttException = mock(MqttException.class);

        // When
        mqttMessageCallback.mqttErrorOccurred(mqttException);

        // Then
        assertTrue(logCaptor.getErrorLogs().contains("Mqtt error occurred"));
    }

    @Test
    void deliveryComplete() {
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
    void connectComplete() {
        // Given
        boolean reconnect = true;
        String serverURI = "tcp://test.com";

        // When
        mqttMessageCallback.connectComplete(reconnect, serverURI);

        // Then
        assertTrue(logCaptor.getInfoLogs().stream().anyMatch(log -> log.startsWith("Connected to MQTT broker")));
    }
}
