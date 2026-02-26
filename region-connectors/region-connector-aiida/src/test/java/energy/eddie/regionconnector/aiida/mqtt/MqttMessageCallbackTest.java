// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.mqtt;

import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.aiida.AiidaConnectionStatusMessageDto;
import energy.eddie.api.agnostic.aiida.AiidaRecordDto;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import energy.eddie.regionconnector.aiida.AiidaBeanConfig;
import energy.eddie.regionconnector.aiida.mqtt.callback.MqttMessageCallback;
import energy.eddie.regionconnector.aiida.mqtt.message.processor.AiidaMessageProcessor;
import energy.eddie.regionconnector.aiida.mqtt.message.processor.AiidaMessageProcessorRegistry;
import energy.eddie.regionconnector.aiida.mqtt.message.processor.data.cim.v1_12.AcknowledgementCimMessageProcessor;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
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
    private static final Sinks.Many<energy.eddie.cim.v1_04.rtd.RTDEnvelope> nearRealTimeDataSinkCimV104 = Sinks.many()
                                                                                                               .unicast()
                                                                                                               .onBackpressureBuffer();
    private static final Sinks.Many<energy.eddie.cim.v1_12.rtd.RTDEnvelope> nearRealTimeDataSinkCimV112 = Sinks.many()
                                                                                                               .unicast()
                                                                                                               .onBackpressureBuffer();
    private static final Sinks.Many<AcknowledgementEnvelope> acknowledgementSinkCim = Sinks.many()
                                                                                           .unicast()
                                                                                           .onBackpressureBuffer();
    private static final Sinks.Many<RawDataMessage> rawDataMessageSink = Sinks.many()
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
        var topic = statusTopic();
        var connectionStatusMessage = getAiidaConnectionStatusMessage(PermissionProcessStatus.REVOKED);

        when(mockObjectMapper.readValue(any(byte[].class), eq(AiidaConnectionStatusMessageDto.class)))
                .thenReturn(connectionStatusMessage);

        // When
        StepVerifier.create(statusSink.asFlux())
                    .then(() -> messageArrived(topic))
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
        var topic = statusTopic();
        var connectionStatusMessage = getAiidaConnectionStatusMessage(PermissionProcessStatus.ACCEPTED);

        when(mockObjectMapper.readValue(any(byte[].class), eq(AiidaConnectionStatusMessageDto.class)))
                .thenReturn(connectionStatusMessage);

        // When
        StepVerifier.create(statusSink.asFlux())
                    .then(() -> messageArrived(topic))
                    .assertNext(msg -> {
                        assertEquals(connectionStatusMessage, msg);
                        assertEquals(PermissionProcessStatus.ACCEPTED, msg.status());
                    })
                    .thenCancel()
                    .verify();
    }

    @Test
    void messageArrived_smartMeterP1RawMessage_valid() {
        // Given
        var topic = schemaTopic(AiidaSchema.SMART_METER_P1_RAW);
        var aiidaRecordDto = getAiidaRecordDto();
        var payload = realObjectMapper.writeValueAsString(aiidaRecordDto);

        var permission = acceptedPermission(LocalDate.now(ZoneId.systemDefault()).minusDays(1),
                                            LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        when(permission.permissionId()).thenReturn(PERMISSION_ID.toString());

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

    @ParameterizedTest(name = "{0}")
    @MethodSource("aiidaSchemaCimScenarios")
    <T> void messageArrived_aiidaSchemaCim_valid(AiidaSchemaScenario<T> scenario) {
        // Given
        var topic = schemaTopic(scenario.aiidaSchema());

        when(mockObjectMapper.readValue(any(byte[].class),
                                        eq(scenario.dataClass()))).thenReturn(scenario.data());

        var permission = acceptedPermission(LocalDate.now(ZoneId.systemDefault()).minusDays(1),
                                            LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        when(permissionRequestViewRepository.findByPermissionId(PERMISSION_ID.toString()))
                .thenReturn(Optional.of(permission));

        // When
        StepVerifier.create(scenario.flux())
                    .then(() -> messageArrived(topic))
                    .assertNext(msg -> assertEquals(scenario.data(), msg))
                    .thenCancel()
                    .verify();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("aiidaSchemaCimScenarios")
    <T> void messageArrived_aiidaSchemaCim_invalidPermission(AiidaSchemaScenario<T> scenario) {
        // Given
        var topic = schemaTopic(scenario.aiidaSchema());

        when(mockObjectMapper.readValue(any(byte[].class),
                                        eq(scenario.dataClass()))).thenReturn(scenario.data());

        when(permissionRequestViewRepository.findByPermissionId(PERMISSION_ID.toString())).thenReturn(Optional.empty());

        // When
        messageArrived(topic);

        // Then
        assertFalse(logCaptor.getErrorLogs().isEmpty());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("aiidaSchemaCimScenarios")
    <T> void messageArrived_aiidaSchemaCim_invalidStatus(AiidaSchemaScenario<T> scenario) {
        // Given
        var topic = schemaTopic(scenario.aiidaSchema());

        when(mockObjectMapper.readValue(any(byte[].class),
                                        eq(scenario.dataClass()))).thenReturn(scenario.data());

        var permission = mock(AiidaPermissionRequest.class);
        when(permission.status()).thenReturn(PermissionProcessStatus.REVOKED);
        when(permissionRequestViewRepository.findByPermissionId(PERMISSION_ID.toString()))
                .thenReturn(Optional.of(permission));

        // When
        messageArrived(topic);

        // Then
        assertFalse(logCaptor.getErrorLogs().isEmpty());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("aiidaSchemaCimScenarios")
    <T> void messageArrived_aiidaSchemaCim_beforeStartDate(AiidaSchemaScenario<T> scenario) {
        // Given
        var topic = schemaTopic(scenario.aiidaSchema());

        var startDate = LocalDate.now(ZoneId.systemDefault()).plusDays(1);
        var endDate = LocalDate.now(ZoneId.systemDefault()).plusDays(10);

        when(mockObjectMapper.readValue(any(byte[].class),
                                        eq(scenario.dataClass()))).thenReturn(scenario.data());

        var permission = acceptedPermission(startDate, endDate);
        when(permissionRequestViewRepository.findByPermissionId(PERMISSION_ID.toString()))
                .thenReturn(Optional.of(permission));

        // When
        messageArrived(topic);

        // Then
        assertFalse(logCaptor.getErrorLogs().isEmpty());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("aiidaSchemaCimScenarios")
    <T> void messageArrived_aiidaSchemaCim_afterEndDate(AiidaSchemaScenario<T> scenario) {
        // Given
        var topic = schemaTopic(scenario.aiidaSchema());
        var startDate = LocalDate.now(ZoneId.systemDefault()).minusDays(10);
        var endDate = LocalDate.now(ZoneId.systemDefault()).minusDays(1);

        when(mockObjectMapper.readValue(any(byte[].class),
                                        eq(scenario.dataClass()))).thenReturn(scenario.data());

        var permission = acceptedPermission(startDate, endDate);
        when(permissionRequestViewRepository.findByPermissionId(PERMISSION_ID.toString()))
                .thenReturn(Optional.of(permission));

        // When
        messageArrived(topic);

        // Then
        assertFalse(logCaptor.getErrorLogs().isEmpty());
    }

    @Test
    void messageArrived_unknownTopic() {
        // Given
        logCaptor.setLogLevelToDebug();
        var topic = MqttTopic.DEFAULT_PREFIX + "/" + PERMISSION_ID + "/data/outbound/unknown";

        // When
        messageArrived(topic);

        // Then
        assertFalse(logCaptor.getDebugLogs().isEmpty());
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
    void mqttErrorOccurred() {
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

    private static Stream<Arguments> aiidaSchemaCimScenarios() {
        var rtdEnvelopeV104 = new energy.eddie.cim.v1_04.rtd.RTDEnvelope()
                .withMessageDocumentHeaderMetaInformationPermissionId(PERMISSION_ID.toString());

        var rtdEnvelopeV112 = new energy.eddie.cim.v1_12.rtd.RTDEnvelope().withMessageDocumentHeader(
                new energy.eddie.cim.v1_12.rtd.MessageDocumentHeader().withMetaInformation(
                        new energy.eddie.cim.v1_12.rtd.MetaInformation()
                                .withRequestPermissionId(PERMISSION_ID.toString())));

        var ackEnvelope = new AcknowledgementEnvelope().withMessageDocumentHeader(
                new energy.eddie.cim.v1_12.ack.MessageDocumentHeader().withMetaInformation(
                        new energy.eddie.cim.v1_12.ack.MetaInformation()
                                .withRequestPermissionId(PERMISSION_ID.toString())));

        return Stream.of(
                Arguments.of(new AiidaSchemaScenario<>(
                        AiidaSchema.SMART_METER_P1_CIM_V1_04,
                        nearRealTimeDataSinkCimV104.asFlux(),
                        rtdEnvelopeV104,
                        energy.eddie.cim.v1_04.rtd.RTDEnvelope.class)),
                Arguments.of(new AiidaSchemaScenario<>(
                        AiidaSchema.SMART_METER_P1_CIM_V1_12,
                        nearRealTimeDataSinkCimV112.asFlux(),
                        rtdEnvelopeV112,
                        energy.eddie.cim.v1_12.rtd.RTDEnvelope.class)),
                Arguments.of(new AiidaSchemaScenario<>(
                        AiidaSchema.ACKNOWLEDGEMENT_CIM_V1_12,
                        acknowledgementSinkCim.asFlux(),
                        ackEnvelope,
                        AcknowledgementEnvelope.class))
        );
    }

    private @NotNull List<AiidaMessageProcessor> getAiidaMessageProcessors() {
        var statusMessageProcessor = new StatusMessageProcessor(permissionRequestViewRepository,
                                                                mockObjectMapper,
                                                                statusSink);
        var rawDataMessageProcessor = new RawDataMessageProcessor(permissionRequestViewRepository,
                                                                  mockObjectMapper,
                                                                  rawDataMessageSink);
        var rtdCimDataMessageProcessorV104 = new energy.eddie.regionconnector.aiida.mqtt.message.processor.data.cim.v1_04.NearRealTimeDataCimMessageProcessor(
                permissionRequestViewRepository,
                mockObjectMapper,
                nearRealTimeDataSinkCimV104);
        var rtdCimDataMessageProcessorV112 = new energy.eddie.regionconnector.aiida.mqtt.message.processor.data.cim.v1_12.NearRealTimeDataCimMessageProcessor(
                permissionRequestViewRepository,
                mockObjectMapper,
                nearRealTimeDataSinkCimV112);
        var ackCimDataMessageProcessor = new AcknowledgementCimMessageProcessor(
                permissionRequestViewRepository,
                mockObjectMapper,
                acknowledgementSinkCim);

        return List.of(statusMessageProcessor,
                       rawDataMessageProcessor,
                       rtdCimDataMessageProcessorV104,
                       rtdCimDataMessageProcessorV112,
                       ackCimDataMessageProcessor);
    }

    private AiidaConnectionStatusMessageDto getAiidaConnectionStatusMessage(PermissionProcessStatus status) {
        var aiidaConnectionStatusMessageJson = "{\"connectionId\":\"30\",\"dataNeedId\":\"00000000-0000-0000-0000-000000000001\",\"timestamp\":1725458241.237425343,\"status\":\"" + status + "\",\"permissionId\":\"" + PERMISSION_ID + "\",\"eddieId\":\"00000000-0000-0000-0000-000000000002\"}";
        return realObjectMapper.readValue(aiidaConnectionStatusMessageJson, AiidaConnectionStatusMessageDto.class);
    }

    private AiidaRecordDto getAiidaRecordDto() {
        var aiidaRecordDtoJson = "{\"asset\":\"SUBMETER\",\"userId\":\"5211ea05-d4ab-48ff-8613-8f4791a56606\",\"dataSourceId\":\"4211ea05-d4ab-48ff-8613-8f4791a56606\",\"permissionId\":\"" + PERMISSION_ID + "\",\"values\":[{\"rawTag\":\"PAPP\",\"dataTag\":\"1-0:1.7.0\",\"rawValue\":\"10\",\"value\":\"10\",\"rawUnitOfMeasurement\":\"VA\",\"unitOfMeasurement\":\"VA\"},{\"rawTag\":\"BASE\",\"dataTag\":\"1-0:1.8.0\",\"rawValue\":\"50\",\"value\":\"50\",\"rawUnitOfMeasurement\":\"Wh\",\"unitOfMeasurement\":\"Wh\"}]}";
        return realObjectMapper.readValue(aiidaRecordDtoJson, AiidaRecordDto.class);
    }

    private String statusTopic() {
        return MqttTopic.DEFAULT_PREFIX + "/" + PERMISSION_ID + "/" + MqttTopicType.STATUS.baseTopicName();
    }

    private String schemaTopic(AiidaSchema aiidaSchema) {
        return MqttTopic.DEFAULT_PREFIX + "/" + PERMISSION_ID + "/" +
               aiidaSchema.buildTopicPath(MqttTopicType.OUTBOUND_DATA.baseTopicName());
    }

    private AiidaPermissionRequest acceptedPermission(LocalDate startDate, LocalDate endDate) {
        var permission = mock(AiidaPermissionRequest.class);
        when(permission.status()).thenReturn(PermissionProcessStatus.ACCEPTED);
        when(permission.start()).thenReturn(startDate);
        when(permission.end()).thenReturn(endDate);
        return permission;
    }

    private void messageArrived(String topic) {
        try {
            mqttMessageCallback.messageArrived(topic, new MqttMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private record AiidaSchemaScenario<T>(
            AiidaSchema aiidaSchema,
            Flux<T> flux,
            T data,
            Class<T> dataClass
    ) {
    }
}
