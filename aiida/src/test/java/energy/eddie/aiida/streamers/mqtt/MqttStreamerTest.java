// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.streamers.mqtt;

import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.models.record.FailedToSendEntity;
import energy.eddie.aiida.models.record.PermissionLatestRecordMap;
import energy.eddie.aiida.repositories.FailedToSendRepository;
import energy.eddie.api.agnostic.aiida.AiidaConnectionStatusMessageDto;
import energy.eddie.api.agnostic.aiida.mqtt.MqttDto;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import energy.eddie.dataneeds.needs.aiida.AiidaSchema;
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
import tools.jackson.databind.ObjectMapper;

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
    private static final String EXPECTED_TERMINATION_TOPIC = "aiida/v1/permission-id/termination";
    private static final UUID AIIDA_ID = UUID.fromString("3211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID DATA_SOURCE_ID = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID USER_ID = UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID PERMISSION_ID = UUID.fromString("6211ea05-d4ab-48ff-8613-8f4791a56606");

    private final TestPublisher<AiidaRecord> recordPublisher = TestPublisher.create();
    private final Sinks.One<UUID> terminationSink = Sinks.one();
    private final AiidaRecord record1 = new AiidaRecord(Instant.now(),
                                                        AiidaAsset.SUBMETER,
                                                        USER_ID,
                                                        DATA_SOURCE_ID,
                                                        List.of(new AiidaRecordValue("1-0:1.8.0",
                                                                                     POSITIVE_ACTIVE_ENERGY,
                                                                                     "444",
                                                                                     KILO_WATT_HOUR,
                                                                                     "10",
                                                                                     KILO_WATT_HOUR)));
    private final AiidaRecord record2 = new AiidaRecord(Instant.now(),
                                                        AiidaAsset.SUBMETER,
                                                        USER_ID,
                                                        DATA_SOURCE_ID,
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
    private MqttStreamingConfig mqttStreamingConfig;
    private MqttStreamer streamer;

    @BeforeEach()
    void setUp() {
        var mqttDto = new MqttDto("tcp://localhost:1883",
                                  PERMISSION_ID.toString(),
                                  "mqttPassword",
                                  DATA_TOPIC,
                                  EXPECTED_STATUS_TOPIC,
                                  EXPECTED_TERMINATION_TOPIC);

        mqttStreamingConfig = new MqttStreamingConfig(mqttDto);
        Permission permissionMock = mock(Permission.class);
        when(mockClient.getPendingTokens()).thenReturn(new IMqttToken[]{});
        var streamingContext = new MqttStreamingContext(mockClient, mqttStreamingConfig, mockLatestRecordMap);

        streamer = new MqttStreamer(AIIDA_ID,
                                    mockRepository,
                                    mockMapper,
                                    permissionMock,
                                    recordPublisher.flux(),
                                    streamingContext,
                                    terminationSink);
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
    void givenConnectComplete_subscribesToTerminationTopic() throws MqttException {
        // Given
        streamer.connect();

        // When
        streamer.connectComplete(false, "testFoo");

        // Then
        verify(mockClient).subscribe(EXPECTED_TERMINATION_TOPIC, 2);
    }

    @Test
    @SuppressWarnings("java:S2925")
    void givenAiidaRecord_sendsViaMqtt() throws MqttException, InterruptedException {
        useReflectionToSetPermissionMock();

        // Given
        streamer.connect();
        // manually call callback
        streamer.connectComplete(false, "fooTest");

        // When
        recordPublisher.next(record1, record2);

        // Need to sleep because messages are handled on different thread and therefore a race condition may occur otherwise
        Thread.sleep(200);

        // Then
        verify(mockClient).setCallback(any());
        verify(mockClient).subscribe(EXPECTED_TERMINATION_TOPIC, 2);
        verify(mockClient, times(2)).publish(eq(EXPECTED_DATA_TOPIC), any(), eq(1), eq(false));
    }

    @Test
    void verify_close_emitsEmptyOnTerminationOne_andClosesClient() throws MqttException {
        // Given
        when(mockClient.disconnect(anyLong())).thenReturn(mockDisconnectToken);
        streamer.connect();
        StepVerifier stepVerifier = StepVerifier.create(terminationSink.asMono()).expectComplete().verifyLater();

        // When
        streamer.close();

        // Then
        stepVerifier.verify(Duration.ofSeconds(2));
        verify(mockClient).disconnect(anyLong());
        verify(mockClient).close();
    }

    @Test
    void givenTerminationRequest_publishesOnMono() throws MqttException {
        // Given
        when(mockMessage.getPayload()).thenReturn(PERMISSION_ID.toString().getBytes(StandardCharsets.UTF_8));
        when(mockClient.disconnect(anyLong())).thenReturn(mockDisconnectToken);
        StepVerifier stepVerifier = StepVerifier.create(terminationSink.asMono())
                                                .expectNext(PERMISSION_ID)
                                                .then(streamer::close)
                                                .expectComplete()
                                                .verifyLater();
        streamer.connect();
        streamer.connectComplete(false, "fooTest");

        // When
        streamer.messageArrived(EXPECTED_TERMINATION_TOPIC, mockMessage);

        // Then
        stepVerifier.verify(Duration.ofSeconds(2));
    }

    @Test
    void givenTerminationRequestWithInvalidPermissionId_doesNotPublishOnMono() throws MqttException {
        // Given
        when(mockMessage.getPayload()).thenReturn("82831e2c-a01c-41b8-9db6-3f51670df7a5".getBytes(StandardCharsets.UTF_8));
        when(mockClient.disconnect(anyLong())).thenReturn(mockDisconnectToken);
        StepVerifier stepVerifier = StepVerifier.create(terminationSink.asMono())
                                                .then(streamer::close)
                                                .expectComplete()
                                                .verifyLater();
        streamer.connect();
        streamer.connectComplete(false, "fooTest");

        // When
        streamer.messageArrived(EXPECTED_TERMINATION_TOPIC, mockMessage);

        // Then
        stepVerifier.verify(Duration.ofSeconds(2));
    }

    @Test
    @SuppressWarnings("java:S2925")
    void givenAiidaRecordAfterTerminationRequest_doesNotSendViaMqtt() throws MqttException, InterruptedException {
        // Given
        streamer.connect();
        // manually call callback
        streamer.connectComplete(false, "fooTest");
        when(mockMessage.getPayload()).thenReturn(PERMISSION_ID.toString().getBytes(StandardCharsets.UTF_8));

        // When
        streamer.messageArrived(EXPECTED_TERMINATION_TOPIC, mockMessage);
        recordPublisher.next(record1, record2);
        Thread.sleep(200);

        // Then
        verify(mockClient, never()).publish(any(), any(), anyInt(), anyBoolean());
    }

    @Test
    @SuppressWarnings("java:S2925")
    void givenExceptionWhileSending_savesToRepository() throws MqttException, InterruptedException {
        useReflectionToSetPermissionMock();

        // Given
        when(mockClient.publish(any(), any(), anyInt(), anyBoolean())).thenThrow(new MqttException(999));
        var json = "MyJson".getBytes(StandardCharsets.UTF_8);
        when(mockMapper.writeValueAsBytes(any())).thenReturn(json);
        streamer.connect();

        // When
        recordPublisher.next(record1);
        Thread.sleep(200);

        // Then
        verify(mockRepository).save(any());
    }

    @Test
    void verify_closeTerminally_publishesSynchronously_andDeletesFailedToSendMessages() throws MqttException {
        // Given
        StepVerifier stepVerifier = StepVerifier.create(terminationSink.asMono()).expectComplete().verifyLater();
        var json = "MyJson".getBytes(StandardCharsets.UTF_8);
        when(mockMapper.writeValueAsBytes(any())).thenReturn(json);
        when(mockClient.publish(anyString(), any(), anyInt(), anyBoolean())).thenReturn(mockPublishToken);
        when(mockClient.disconnect(anyLong())).thenReturn(mockDisconnectToken);
        streamer.connect();

        // When
        streamer.closeTerminally(mockStatusMessage);

        // Then
        stepVerifier.verify(Duration.ofSeconds(2));
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

    private void useReflectionToSetPermissionMock() {
        try {
            var field = streamer.getClass().getDeclaredField("permission");
            field.setAccessible(true);
            var permissionReflection = (Permission) field.get(streamer);
            var dataNeed = mock(AiidaLocalDataNeed.class);
            when(dataNeed.schemas()).thenReturn(Set.of(AiidaSchema.SMART_METER_P1_RAW));
            when(permissionReflection.dataNeed()).thenReturn(dataNeed);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
