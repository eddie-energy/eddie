// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.mqtt;

import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.aiida.AiidaConnectionStatusMessageDto;
import energy.eddie.api.agnostic.aiida.AiidaRecordDto;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import energy.eddie.regionconnector.aiida.AiidaBeanConfig;
import energy.eddie.regionconnector.aiida.exceptions.PermissionInvalidException;
import energy.eddie.regionconnector.aiida.mqtt.callback.MqttMessageCallback;
import energy.eddie.regionconnector.aiida.mqtt.message.processor.AiidaMessageProcessor;
import energy.eddie.regionconnector.aiida.mqtt.message.processor.AiidaMessageProcessorRegistry;
import energy.eddie.regionconnector.aiida.mqtt.message.processor.data.raw.RawDataMessageProcessor;
import energy.eddie.regionconnector.aiida.mqtt.message.processor.status.StatusMessageProcessor;
import energy.eddie.regionconnector.aiida.mqtt.topic.MqttTopic;
import energy.eddie.regionconnector.aiida.mqtt.topic.MqttTopicType;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionRequestViewRepository;
import nl.altindag.log.LogCaptor;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqttMessageCallbackTest {
    private static final UUID PERMISSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    // Sinks
    private final Sinks.Many<AiidaConnectionStatusMessageDto> statusSink = Sinks.many()
                                                                                .unicast()
                                                                                .onBackpressureBuffer();
    private final Sinks.Many<energy.eddie.cim.v1_04.rtd.RTDEnvelope> nearRealTimeDataSinkCimV104 = Sinks.many()
                                                                                                        .unicast()
                                                                                                        .onBackpressureBuffer();
    private final Sinks.Many<energy.eddie.cim.v1_12.rtd.RTDEnvelope> nearRealTimeDataSinkCimV112 = Sinks.many()
                                                                                                        .unicast()
                                                                                                        .onBackpressureBuffer();
    private final Sinks.Many<RawDataMessage> rawDataMessageSink = Sinks.many()
                                                                       .unicast()
                                                                       .onBackpressureBuffer();
    private final LogCaptor logCaptor = LogCaptor.forClass(MqttMessageCallback.class);
    private MqttMessageCallback mqttMessageCallback;

    @Mock
    private ObjectMapper mockObjectMapper;
    @Mock
    private AiidaPermissionRequestViewRepository permissionRequestViewRepository;

    private ObjectMapper realObjectMapper;

    @BeforeEach
    void setUp() {
        var builder = JsonMapper.builder();
        new AiidaBeanConfig().objectMapperCustomizer().customize(builder);
        realObjectMapper = builder.build();

        // Message Processors
        var messageProcessorRegistry = new AiidaMessageProcessorRegistry(getAiidaMessageProcessors());
        mqttMessageCallback = new MqttMessageCallback(messageProcessorRegistry);
    }

    @AfterEach
    void tearDown() {
        logCaptor.clearLogs();
        logCaptor.resetLogLevel();
    }

    @Test
    void messageArrived_statusMessage_revoked() {
        // Given
        var topic = MqttTopic.defaultPrefix() + "/" + PERMISSION_ID + "/" + MqttTopicType.STATUS.baseTopicName();
        var connectionStatusMessage = getAiidaConnectionStatusMessage(PermissionProcessStatus.REVOKED);

        when(mockObjectMapper.readValue(any(byte[].class), eq(AiidaConnectionStatusMessageDto.class)))
                .thenReturn(connectionStatusMessage);

        // When
        StepVerifier.create(statusSink.asFlux())
                    .then(() -> {
                        try {
                            mqttMessageCallback.messageArrived(topic, new MqttMessage());
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
    void messageArrived_statusMessage_accepted() {
        // Given
        var topic = MqttTopic.defaultPrefix() + "/" + PERMISSION_ID + "/" + MqttTopicType.STATUS.baseTopicName();
        var connectionStatusMessage = getAiidaConnectionStatusMessage(PermissionProcessStatus.ACCEPTED);

        when(mockObjectMapper.readValue(any(byte[].class), eq(AiidaConnectionStatusMessageDto.class)))
                .thenReturn(connectionStatusMessage);

        // When
        StepVerifier.create(statusSink.asFlux())
                    .then(() -> {
                        try {
                            mqttMessageCallback.messageArrived(topic, new MqttMessage());
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
    void messageArrived_smartMeterP1CimMessage_valid() {
        // Given
        var topic = MqttTopic.defaultPrefix() + "/" + PERMISSION_ID + "/" +
                    AiidaSchema.SMART_METER_P1_CIM_V1_04.buildTopicPath(MqttTopicType.OUTBOUND_DATA.baseTopicName());

        var rtdEnvelope = new RTDEnvelope();
        rtdEnvelope.withMessageDocumentHeaderMetaInformationPermissionId(PERMISSION_ID.toString());
        when(mockObjectMapper.readValue(any(byte[].class), eq(RTDEnvelope.class))).thenReturn(rtdEnvelope);

        var permission = mock(AiidaPermissionRequest.class);
        when(permission.status())
                .thenReturn(PermissionProcessStatus.ACCEPTED);
        when(permission.start())
                .thenReturn(LocalDate.now(ZoneId.systemDefault()).minusDays(1));
        when(permission.end())
                .thenReturn(LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        when(permissionRequestViewRepository.findByPermissionId(PERMISSION_ID.toString()))
                .thenReturn(Optional.of(permission));

        // When
        StepVerifier.create(nearRealTimeDataSinkCimV104.asFlux())
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
    void messageArrived_smartMeterP1CimMessage_invalidPermission() {
        // Given
        var topic = MqttTopic.defaultPrefix() + "/" + PERMISSION_ID + "/" +
                    AiidaSchema.SMART_METER_P1_CIM_V1_04.buildTopicPath(MqttTopicType.OUTBOUND_DATA.baseTopicName());

        var rtdEnvelope = new RTDEnvelope();
        rtdEnvelope.withMessageDocumentHeaderMetaInformationPermissionId(PERMISSION_ID.toString());
        when(mockObjectMapper.readValue(any(byte[].class), eq(RTDEnvelope.class))).thenReturn(rtdEnvelope);

        when(permissionRequestViewRepository.findByPermissionId(PERMISSION_ID.toString())).thenReturn(Optional.empty());

        var expectedErrorLog = "No permission with ID '" + PERMISSION_ID + "' found.";

        // When
        mqttMessageCallback.messageArrived(topic, new MqttMessage());

        // Then
        assertEquals(expectedErrorLog, logCaptor.getErrorLogs().getFirst());
    }

    @Test
    void messageArrived_smartMeterP1CimMessage_invalidStatus() {
        // Given
        var topic = MqttTopic.defaultPrefix() + "/" + PERMISSION_ID + "/" +
                    AiidaSchema.SMART_METER_P1_CIM_V1_04.buildTopicPath(MqttTopicType.OUTBOUND_DATA.baseTopicName());

        var rtdEnvelope = new RTDEnvelope();
        rtdEnvelope.withMessageDocumentHeaderMetaInformationPermissionId(PERMISSION_ID.toString());
        when(mockObjectMapper.readValue(any(byte[].class), eq(RTDEnvelope.class))).thenReturn(rtdEnvelope);

        var permission = mock(AiidaPermissionRequest.class);
        when(permission.permissionId())
                .thenReturn(PERMISSION_ID.toString());
        when(permission.status())
                .thenReturn(PermissionProcessStatus.REVOKED);
        when(permissionRequestViewRepository.findByPermissionId(PERMISSION_ID.toString()))
                .thenReturn(Optional.of(permission));
        var expectedErrorLog = "Permission with ID '" + PERMISSION_ID + "' is invalid: Permission status is not ACCEPTED but REVOKED";

        // When
        mqttMessageCallback.messageArrived(topic, new MqttMessage());

        // Then
        assertEquals(expectedErrorLog, logCaptor.getErrorLogs().getFirst());
    }

    @Test
    void messageArrived_smartMeterP1CimMessage_beforeStartDate() {
        // Given
        var topic = MqttTopic.defaultPrefix() + "/" + PERMISSION_ID + "/" +
                    AiidaSchema.SMART_METER_P1_CIM_V1_04.buildTopicPath(MqttTopicType.OUTBOUND_DATA.baseTopicName());

        var startDate = LocalDate.now(ZoneId.systemDefault()).plusDays(1);
        var endDate = LocalDate.now(ZoneId.systemDefault()).plusDays(10);
        var rtdEnvelope = new RTDEnvelope();
        rtdEnvelope.withMessageDocumentHeaderMetaInformationPermissionId(PERMISSION_ID.toString());
        when(mockObjectMapper.readValue(any(byte[].class), eq(RTDEnvelope.class))).thenReturn(rtdEnvelope);

        var permission = mock(AiidaPermissionRequest.class);
        when(permission.permissionId())
                .thenReturn(PERMISSION_ID.toString());
        when(permission.status())
                .thenReturn(PermissionProcessStatus.ACCEPTED);
        when(permission.start())
                .thenReturn(startDate);
        when(permission.end())
                .thenReturn(endDate);
        when(permissionRequestViewRepository.findByPermissionId(PERMISSION_ID.toString()))
                .thenReturn(Optional.of(permission));

        var expectedErrorMessage = "Current date is outside of permission timespan (" + startDate + " - " + endDate + ")";
        var expectedErrorLog = new PermissionInvalidException(PERMISSION_ID.toString(), expectedErrorMessage);

        // When
        mqttMessageCallback.messageArrived(topic, new MqttMessage());

        // Then
        assertEquals(expectedErrorLog.getMessage(), logCaptor.getErrorLogs().getFirst());
    }

    @Test
    void messageArrived_smartMeterP1CimMessage_afterEndDate() {
        // Given
        var topic = MqttTopic.defaultPrefix() + "/" + PERMISSION_ID + "/" +
                    AiidaSchema.SMART_METER_P1_CIM_V1_04.buildTopicPath(MqttTopicType.OUTBOUND_DATA.baseTopicName());
        var startDate = LocalDate.now(ZoneId.systemDefault()).minusDays(10);
        var endDate = LocalDate.now(ZoneId.systemDefault()).minusDays(1);
        var rtdEnvelope = new RTDEnvelope();
        rtdEnvelope.withMessageDocumentHeaderMetaInformationPermissionId(PERMISSION_ID.toString());
        when(mockObjectMapper.readValue(any(byte[].class), eq(RTDEnvelope.class))).thenReturn(rtdEnvelope);

        var permission = mock(AiidaPermissionRequest.class);
        when(permission.permissionId())
                .thenReturn(PERMISSION_ID.toString());
        when(permission.status())
                .thenReturn(PermissionProcessStatus.ACCEPTED);
        when(permission.start())
                .thenReturn(startDate);
        when(permission.end())
                .thenReturn(endDate);
        when(permissionRequestViewRepository.findByPermissionId(PERMISSION_ID.toString()))
                .thenReturn(Optional.of(permission));

        var expectedErrorMessage = "Current date is outside of permission timespan (" + startDate + " - " + endDate + ")";
        var expectedErrorLog = new PermissionInvalidException(PERMISSION_ID.toString(), expectedErrorMessage);

        // When
        mqttMessageCallback.messageArrived(topic, new MqttMessage());

        // Then
        assertEquals(expectedErrorLog.getMessage(), logCaptor.getErrorLogs().getFirst());
    }

    @Test
    void messageArrived_smartMeterP1RawMessage_valid() {
        // Given
        var topic = MqttTopic.defaultPrefix() + "/" + PERMISSION_ID + "/" +
                    AiidaSchema.SMART_METER_P1_RAW.buildTopicPath(MqttTopicType.OUTBOUND_DATA.baseTopicName());
        var aiidaRecordDto = getAiidaRecordDto();
        var payload = realObjectMapper.writeValueAsString(aiidaRecordDto);

        var permission = mock(AiidaPermissionRequest.class);
        when(permission.permissionId())
                .thenReturn(PERMISSION_ID.toString());
        when(permission.status())
                .thenReturn(PermissionProcessStatus.ACCEPTED);
        when(permission.start())
                .thenReturn(LocalDate.now(ZoneId.systemDefault()).minusDays(1));
        when(permission.end())
                .thenReturn(LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        when(permissionRequestViewRepository.findByPermissionId(PERMISSION_ID.toString()))
                .thenReturn(Optional.of(permission));
        when(mockObjectMapper.readValue(any(byte[].class), eq(AiidaRecordDto.class)))
                .thenReturn(aiidaRecordDto);

        var mqttMessage = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));

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
                        assertEquals(PERMISSION_ID.toString(), msg.permissionId());
                        assertEquals(payload, msg.rawPayload());
                    })
                    .thenCancel()
                    .verify();
    }

    @Test
    void messageArrived_smartMeterP1RawMessage_invalidTopic() {
        // Given
        var topic = "invalid/v1/" + PERMISSION_ID + "/" +
                    AiidaSchema.SMART_METER_P1_RAW.buildTopicPath(MqttTopicType.OUTBOUND_DATA.baseTopicName());
        var expectedErrorMessage = "No AiidaMessageProcessor found for topic " + topic;

        // When
        mqttMessageCallback.messageArrived(topic, new MqttMessage());

        // Then
        assertEquals(expectedErrorMessage, logCaptor.getErrorLogs().getFirst());
    }

    @Test
    void messageArrived_smartMeterP1RawMessage_invalidPermission() {
        // Given
        var topic = MqttTopic.defaultPrefix() + "/" + PERMISSION_ID + "/" +
                    AiidaSchema.SMART_METER_P1_RAW.buildTopicPath(MqttTopicType.OUTBOUND_DATA.baseTopicName());
        var aiidaRecordDto = getAiidaRecordDto();
        var expectedErrorLog = "No permission with ID '" + PERMISSION_ID + "' found.";

        when(permissionRequestViewRepository.findByPermissionId(PERMISSION_ID.toString()))
                .thenReturn(Optional.empty());
        when(mockObjectMapper.readValue(any(byte[].class), eq(AiidaRecordDto.class)))
                .thenReturn(aiidaRecordDto);

        // When
        mqttMessageCallback.messageArrived(topic, new MqttMessage());

        // Then
        assertEquals(expectedErrorLog, logCaptor.getErrorLogs().getFirst());
    }

    @Test
    void messageArrived_smartMeterP1RawMessage_invalidStatus() {
        // Given
        var topic = MqttTopic.defaultPrefix() + "/" + PERMISSION_ID + "/" +
                    AiidaSchema.SMART_METER_P1_RAW.buildTopicPath(MqttTopicType.OUTBOUND_DATA.baseTopicName());
        var aiidaRecordDto = getAiidaRecordDto();
        var expectedErrorLog = "Permission with ID '" + PERMISSION_ID + "' is invalid: Permission status is not ACCEPTED but REVOKED";

        var permission = mock(AiidaPermissionRequest.class);
        when(permission.permissionId())
                .thenReturn(PERMISSION_ID.toString());
        when(permission.status())
                .thenReturn(PermissionProcessStatus.REVOKED);
        when(permissionRequestViewRepository.findByPermissionId(PERMISSION_ID.toString()))
                .thenReturn(Optional.of(permission));
        when(mockObjectMapper.readValue(any(byte[].class), eq(AiidaRecordDto.class)))
                .thenReturn(aiidaRecordDto);

        // When
        mqttMessageCallback.messageArrived(topic, new MqttMessage());

        // Then
        assertEquals(expectedErrorLog, logCaptor.getErrorLogs().getFirst());
    }

    @Test
    void messageArrived_smartMeterP1RawMessage_beforeStartDate() {
        // Given
        var topic = MqttTopic.defaultPrefix() + "/" + PERMISSION_ID + "/" +
                    AiidaSchema.SMART_METER_P1_RAW.buildTopicPath(MqttTopicType.OUTBOUND_DATA.baseTopicName());
        var aiidaRecordDto = getAiidaRecordDto();
        var startDate = LocalDate.now(ZoneId.systemDefault()).plusDays(1);
        var endDate = LocalDate.now(ZoneId.systemDefault()).plusDays(10);

        var permission = mock(AiidaPermissionRequest.class);
        when(permission.permissionId())
                .thenReturn(PERMISSION_ID.toString());
        when(permission.status())
                .thenReturn(PermissionProcessStatus.ACCEPTED);
        when(permission.start())
                .thenReturn(startDate);
        when(permission.end())
                .thenReturn(endDate);
        when(permissionRequestViewRepository.findByPermissionId(PERMISSION_ID.toString()))
                .thenReturn(Optional.of(permission));
        when(mockObjectMapper.readValue(any(byte[].class), eq(AiidaRecordDto.class)))
                .thenReturn(aiidaRecordDto);

        var expectedErrorMessage = "Current date is outside of permission timespan (" + startDate + " - " + endDate + ")";
        var expectedErrorLog = new PermissionInvalidException(PERMISSION_ID.toString(), expectedErrorMessage);

        // When
        mqttMessageCallback.messageArrived(topic, new MqttMessage());

        // Then
        assertEquals(expectedErrorLog.getMessage(), logCaptor.getErrorLogs().getFirst());
    }

    @Test
    void messageArrived_smartMeterP1RawMessage_afterEndDate() {
        // Given
        var topic = MqttTopic.defaultPrefix() + "/" + PERMISSION_ID + "/" +
                    AiidaSchema.SMART_METER_P1_RAW.buildTopicPath(MqttTopicType.OUTBOUND_DATA.baseTopicName());
        var permission = mock(AiidaPermissionRequest.class);
        var aiidaRecordDto = getAiidaRecordDto();
        var startDate = LocalDate.now(ZoneId.systemDefault()).minusDays(10);
        var endDate = LocalDate.now(ZoneId.systemDefault()).minusDays(1);

        when(permission.permissionId())
                .thenReturn(PERMISSION_ID.toString());
        when(permission.status())
                .thenReturn(PermissionProcessStatus.ACCEPTED);
        when(permission.start())
                .thenReturn(startDate);
        when(permission.end())
                .thenReturn(endDate);
        when(permissionRequestViewRepository.findByPermissionId(PERMISSION_ID.toString()))
                .thenReturn(Optional.of(permission));

        when(mockObjectMapper.readValue(any(byte[].class), eq(AiidaRecordDto.class))).thenReturn(aiidaRecordDto);

        var expectedErrorMessage = "Current date is outside of permission timespan (" + startDate + " - " + endDate + ")";
        var expectedErrorLog = new PermissionInvalidException(PERMISSION_ID.toString(), expectedErrorMessage);

        // When
        mqttMessageCallback.messageArrived(topic, new MqttMessage());

        // Then
        assertEquals(expectedErrorLog.getMessage(), logCaptor.getErrorLogs().getFirst());
    }

    @Test
    void messageArrived_unknownTopic() {
        // Given
        var topic = MqttTopic.defaultPrefix() + "/" + PERMISSION_ID + "/data/outbound/unknown";
        var expectedErrorMessage = "No AiidaMessageProcessor found for topic " + topic;

        // When
        mqttMessageCallback.messageArrived(topic, new MqttMessage());

        // Then
        assertEquals(expectedErrorMessage, logCaptor.getErrorLogs().getFirst());
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

    private @NotNull List<AiidaMessageProcessor> getAiidaMessageProcessors() {
        var statusMessageProcessor = new StatusMessageProcessor(permissionRequestViewRepository,
                                                                mockObjectMapper,
                                                                statusSink);
        var rawDataMessageProcessor = new RawDataMessageProcessor(permissionRequestViewRepository,
                                                                  mockObjectMapper,
                                                                  rawDataMessageSink);
        var cimDataMessageProcessorV104 = new energy.eddie.regionconnector.aiida.mqtt.message.processor.data.cim.v1_04.CimDataMessageProcessor(
                permissionRequestViewRepository,
                mockObjectMapper,
                nearRealTimeDataSinkCimV104);
        var cimDataMessageProcessorV112 = new energy.eddie.regionconnector.aiida.mqtt.message.processor.data.cim.v1_12.CimDataMessageProcessor(
                permissionRequestViewRepository,
                mockObjectMapper,
                nearRealTimeDataSinkCimV112);

        return List.of(statusMessageProcessor,
                       rawDataMessageProcessor,
                       cimDataMessageProcessorV104,
                       cimDataMessageProcessorV112);
    }

    private AiidaRecordDto getAiidaRecordDto() {
        var aiidaRecordDtoJson = "{\"asset\":\"SUBMETER\",\"userId\":\"5211ea05-d4ab-48ff-8613-8f4791a56606\",\"dataSourceId\":\"4211ea05-d4ab-48ff-8613-8f4791a56606\",\"permissionId\":\"" + PERMISSION_ID + "\",\"values\":[{\"rawTag\":\"PAPP\",\"dataTag\":\"1-0:1.7.0\",\"rawValue\":\"10\",\"value\":\"10\",\"rawUnitOfMeasurement\":\"VA\",\"unitOfMeasurement\":\"VA\"},{\"rawTag\":\"BASE\",\"dataTag\":\"1-0:1.8.0\",\"rawValue\":\"50\",\"value\":\"50\",\"rawUnitOfMeasurement\":\"Wh\",\"unitOfMeasurement\":\"Wh\"}]}";
        return realObjectMapper.readValue(aiidaRecordDtoJson, AiidaRecordDto.class);
    }

    private AiidaConnectionStatusMessageDto getAiidaConnectionStatusMessage(PermissionProcessStatus status) {
        var aiidaConnectionStatusMessageJson = "{\"connectionId\":\"30\",\"dataNeedId\":\"00000000-0000-0000-0000-000000000001\",\"timestamp\":1725458241.237425343,\"status\":\"" + status + "\",\"permissionId\":\"" + PERMISSION_ID + "\",\"eddieId\":\"00000000-0000-0000-0000-000000000002\"}";
        return realObjectMapper.readValue(aiidaConnectionStatusMessageJson, AiidaConnectionStatusMessageDto.class);
    }
}
