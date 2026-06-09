// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.streamers.mqtt;

import energy.eddie.aiida.application.information.ApplicationInformation;
import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.models.record.FailedToSendEntity;
import energy.eddie.aiida.models.record.PermissionLatestRecordMap;
import energy.eddie.aiida.repositories.FailedToSendRepository;
import energy.eddie.aiida.schemas.rtd.SchemaFormatterRegistry;
import energy.eddie.aiida.schemas.rtd.raw.RawFormatter;
import energy.eddie.aiida.services.ApplicationInformationService;
import energy.eddie.api.agnostic.aiida.AiidaConnectionStatusMessageDto;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.api.agnostic.aiida.mqtt.MqttDto;
import energy.eddie.cim.agnostic.PermissionCommand;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static energy.eddie.api.agnostic.aiida.ObisCode.NEGATIVE_ACTIVE_ENERGY;
import static energy.eddie.api.agnostic.aiida.ObisCode.POSITIVE_ACTIVE_ENERGY;
import static energy.eddie.api.agnostic.aiida.UnitOfMeasurement.KILO_WATT_HOUR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MqttStreamerTest {
    private static final String DATA_TOPIC = "aiida/v1/permission-id/data/outbound";
    private static final String EXPECTED_DATA_TOPIC = DATA_TOPIC + "/smart-meter-p1-raw";
    private static final String EXPECTED_STATUS_TOPIC = "aiida/v1/permission-id/status";
    private static final String EXPECTED_COMMAND_TOPIC = "aiida/v1/permission-id/command";
    private static final String EXPECTED_ACK_TOPIC = "aiida/v1/permission-id/acknowledgement";
    private static final UUID PERMISSION_ID = UUID.fromString("6211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final DataSource DATA_SOURCE = mock(DataSource.class);

    private final TestPublisher<AiidaRecord> recordPublisher = TestPublisher.create();
    private final Sinks.Many<PermissionCommand> commandSink = Sinks.many().multicast().onBackpressureBuffer();
    private final AiidaRecord record1 = new AiidaRecord(Instant.now(),
                                                        DATA_SOURCE,
                                                        List.of(new AiidaRecordValue("1-0:1.8.0",
                                                                                     POSITIVE_ACTIVE_ENERGY,
                                                                                     "444",
                                                                                     KILO_WATT_HOUR,
                                                                                     "10",
                                                                                     KILO_WATT_HOUR)));
    private final AiidaRecord record2 = new AiidaRecord(Instant.now(),
                                                        DATA_SOURCE,
                                                        List.of(new AiidaRecordValue("1-0:2.8.0",
                                                                                     NEGATIVE_ACTIVE_ENERGY,
                                                                                     "888",
                                                                                     KILO_WATT_HOUR,
                                                                                     "10",
                                                                                     KILO_WATT_HOUR)));
    @Mock
    private MqttAsyncClient mockClient;
    @Mock
    private IMqttToken mockDisconnectToken;
    @Mock
    private IMqttToken mockPublishToken;
    @Mock
    private MqttMessage mockMessage;
    @Mock
    private FailedToSendRepository mockRepository;
    @Mock
    private ObjectMapper mockMapper;
    @Mock
    private AiidaConnectionStatusMessageDto mockStatusMessage;
    @Mock
    private PermissionLatestRecordMap mockLatestRecordMap;
    @Mock
    private ApplicationInformationService mockApplicationInformationService;
    @Mock
    private Permission permissionMock;
    private MqttStreamingConfig mqttStreamingConfig;
    private MqttStreamer streamer;

    @BeforeEach()
    void setUp() {
        // Permission
        when(permissionMock.transmissionEnabled()).thenReturn(true);

        // Mqtt Streaming Config
        var mqttDto = new MqttDto("tcp://localhost:1883",
                                  PERMISSION_ID.toString(),
                                  "mqttPassword",
                                  DATA_TOPIC,
                                  EXPECTED_STATUS_TOPIC,
                                  EXPECTED_COMMAND_TOPIC,
                                  EXPECTED_ACK_TOPIC);
        mqttStreamingConfig = new MqttStreamingConfig(mqttDto);

        // MQTT Streaming Context
        when(mockClient.getPendingTokens()).thenReturn(new IMqttToken[]{});
        var streamingContext = new MqttStreamingContext(mockClient, mqttStreamingConfig, mockLatestRecordMap);

        // Application Information
        var applicationInformation = new ApplicationInformation();
        when(mockApplicationInformationService.applicationInformation()).thenReturn(applicationInformation);

        // Schema Formatter Registry
        var builder = JsonMapper.builder();
        new AiidaConfiguration().objectMapperCustomizer().customize(builder);
        var mapper = builder.build();
        var rawSchemaFormatter = new RawFormatter(mockApplicationInformationService, mapper);
        var schemaFormatterRegistry = new SchemaFormatterRegistry(List.of(rawSchemaFormatter));

        streamer = new MqttStreamer(
                mockRepository,
                mockMapper,
                permissionMock,
                recordPublisher.flux(),
                schemaFormatterRegistry,
                streamingContext,
                commandSink);
    }


    @Test
    void verify_connect_setsCallback_andSubscribes() {
        // Given
        recordPublisher.assertNoSubscribers();

        // When
        streamer.connect();

        // Then
        verify(mockClient).setCallback(any());
        recordPublisher.assertSubscribers(1);
    }

    @Test
    void verify_connect_usesUsernameAndPassword() throws MqttException {
        // When
        streamer.connect();

        // Then
        verify(mockClient)
                .connect(argThat(options ->
                                         options.getUserName()
                                                .equals(mqttStreamingConfig.username().toString())
                                         && !options.isCleanStart()
                                         && options.isAutomaticReconnect()
                                         && new String(options.getPassword(), StandardCharsets.UTF_8)
                                                 .equals(mqttStreamingConfig.password()))
                );
    }

    @Test
    void givenConnectComplete_subscribesToCommandTopic() throws MqttException {
        // Given
        streamer.connect();

        // When
        streamer.connectComplete(false, "testFoo");

        // Then
        verify(mockClient).subscribe(EXPECTED_COMMAND_TOPIC, 2);
    }

    @Test
    void givenAiidaRecord_sendsViaMqtt() throws MqttException {
        // Given
        mockDataNeedWithSchemas();


        streamer.connect();

        // manually call callback
        streamer.connectComplete(false, "fooTest");

        // When
        recordPublisher.next(record1, record2);

        // Then
        // records are published on Schedulers.boundedElastic(), so await the async side effects
        verify(mockClient, timeout(2000).times(2)).publish(eq(EXPECTED_DATA_TOPIC), any(), eq(1), eq(false));
        verify(mockClient).setCallback(any());
        verify(mockClient).subscribe(EXPECTED_COMMAND_TOPIC, 2);
    }

    @Test
    void verify_close_disconnectsAndClosesClient() throws MqttException {
        // Given
        when(mockClient.disconnect(anyLong())).thenReturn(mockDisconnectToken);
        streamer.connect();

        // When
        streamer.close();

        // Then
        verify(mockClient).disconnect(anyLong());
        verify(mockClient).close();
    }

    @Test
    void givenTerminateCommand_forwardsToCommandSink() {
        // Given
        var command = new PermissionCommand.Terminate("aiida", PERMISSION_ID);
        when(mockMessage.getPayload()).thenReturn(new byte[]{1});
        when(mockMapper.readValue(any(byte[].class), eq(PermissionCommand.class))).thenReturn(command);
        StepVerifier stepVerifier = StepVerifier.create(commandSink.asFlux())
                                                .expectNext(command)
                                                .thenCancel()
                                                .verifyLater();
        streamer.connect();
        streamer.connectComplete(false, "fooTest");

        // When
        streamer.messageArrived(EXPECTED_COMMAND_TOPIC, mockMessage);

        // Then
        stepVerifier.verify(Duration.ofSeconds(2));
    }

    @Test
    void givenSetTransmissionEnabledCommand_forwardsToCommandSink() {
        // Given
        var command = new PermissionCommand.SetTransmissionEnabled("aiida", PERMISSION_ID, false);
        when(mockMessage.getPayload()).thenReturn(new byte[]{1});
        when(mockMapper.readValue(any(byte[].class), eq(PermissionCommand.class))).thenReturn(command);
        StepVerifier stepVerifier = StepVerifier.create(commandSink.asFlux())
                                                .expectNext(command)
                                                .thenCancel()
                                                .verifyLater();
        streamer.connect();
        streamer.connectComplete(false, "fooTest");

        // When
        streamer.messageArrived(EXPECTED_COMMAND_TOPIC, mockMessage);

        // Then
        stepVerifier.verify(Duration.ofSeconds(2));
    }

    @Test
    void givenUpdateScheduleCommand_forwardsToCommandSink() {
        // Given
        var command = new PermissionCommand.UpdateTransmissionSchedule("aiida", PERMISSION_ID, "0 0 * * * *");
        when(mockMessage.getPayload()).thenReturn(new byte[]{1});
        when(mockMapper.readValue(any(byte[].class), eq(PermissionCommand.class))).thenReturn(command);
        StepVerifier stepVerifier = StepVerifier.create(commandSink.asFlux())
                                                .expectNext(command)
                                                .thenCancel()
                                                .verifyLater();
        streamer.connect();
        streamer.connectComplete(false, "fooTest");

        // When
        streamer.messageArrived(EXPECTED_COMMAND_TOPIC, mockMessage);

        // Then
        stepVerifier.verify(Duration.ofSeconds(2));
    }

    @Test
    void givenUnparseableCommand_doesNotForwardToCommandSink() {
        // Given
        when(mockMessage.getPayload()).thenReturn("not-json".getBytes(StandardCharsets.UTF_8));
        when(mockMapper.readValue(any(byte[].class), eq(PermissionCommand.class)))
                .thenThrow(new JacksonException("boom") {});
        StepVerifier stepVerifier = StepVerifier.create(commandSink.asFlux())
                                                .expectSubscription()
                                                .expectNoEvent(Duration.ofMillis(200))
                                                .thenCancel()
                                                .verifyLater();
        streamer.connect();
        streamer.connectComplete(false, "fooTest");

        // When
        streamer.messageArrived(EXPECTED_COMMAND_TOPIC, mockMessage);

        // Then
        stepVerifier.verify(Duration.ofSeconds(2));
    }

    @Test
    @SuppressWarnings("java:S2925")
    void givenTransmissionDisabled_doesNotSendViaMqtt() throws MqttException, InterruptedException {
        // Given
        mockDataNeedWithSchemas();
        streamer.connect();
        streamer.connectComplete(false, "fooTest");

        // When
        streamer.setTransmissionEnabled(false);
        recordPublisher.next(record1, record2);
        Thread.sleep(200);

        // Then
        verify(mockClient, never()).publish(any(), any(), anyInt(), anyBoolean());
    }

    @Test
    void givenUpdatedRecordFlux_resubscribesAndSendsViaMqtt() throws MqttException {
        // Given
        mockDataNeedWithSchemas();
        streamer.connect();
        streamer.connectComplete(false, "fooTest");
        TestPublisher<AiidaRecord> newPublisher = TestPublisher.create();

        // When
        streamer.updateRecordFlux(newPublisher.flux());
        newPublisher.next(record1);

        // Then
        // record is published on Schedulers.boundedElastic(), so await the async side effect
        verify(mockClient, timeout(2000)).publish(eq(EXPECTED_DATA_TOPIC), any(), eq(1), eq(false));
    }

    @Test
    void givenExceptionWhileSending_savesToRepository() throws Exception {
        // Given
        mockDataNeedWithSchemas();

        when(mockClient.publish(any(), any(), anyInt(), anyBoolean())).thenThrow(new MqttException(999));
        streamer.connect();

        // When
        recordPublisher.next(record1);

        // Then
        // record is published on Schedulers.boundedElastic(), so await the async side effect
        verify(mockRepository, timeout(2000)).save(any());
    }

    @Test
    void verify_closeTerminally_publishesSynchronously_andDeletesFailedToSendMessages() throws MqttException {
        // Given
        var json = "MyJson".getBytes(StandardCharsets.UTF_8);
        when(mockMapper.writeValueAsBytes(any())).thenReturn(json);
        when(mockClient.publish(anyString(), any(), anyInt(), anyBoolean())).thenReturn(mockPublishToken);
        when(mockClient.disconnect(anyLong())).thenReturn(mockDisconnectToken);
        streamer.connect();

        // When
        streamer.closeTerminally(mockStatusMessage);

        // Then
        verify(mockClient).publish(EXPECTED_STATUS_TOPIC, json, 1, true);
        verify(mockPublishToken).waitForCompletion(anyLong());
        verify(mockDisconnectToken).waitForCompletion();
        verify(mockClient).disconnect(anyLong());
        verify(mockClient).close();
        verify(mockRepository).deleteAllByPermissionId(any());
    }

    @Test
    void verify_connectComplete_retransmitFailedMessages() throws MqttException {
        // Given
        var topic = "bar";
        var json = "json".getBytes(StandardCharsets.UTF_8);
        var failedToSendEntity = new FailedToSendEntity(PERMISSION_ID, topic, json);
        when(mockRepository.findAllByPermissionId(PERMISSION_ID))
                .thenReturn(List.of(failedToSendEntity));
        streamer.connect();

        // When
        streamer.connectComplete(false, "tcp://localhost:1883");

        // Then
        verify(mockRepository).deleteAllById(any());
        verify(mockClient, times(1)).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    private void mockDataNeedWithSchemas() {
        var dataNeed = mock(AiidaLocalDataNeed.class);
        when(dataNeed.schemas()).thenReturn(Set.of(AiidaSchema.SMART_METER_P1_RAW));
        when(permissionMock.dataNeed()).thenReturn(dataNeed);
    }
}
