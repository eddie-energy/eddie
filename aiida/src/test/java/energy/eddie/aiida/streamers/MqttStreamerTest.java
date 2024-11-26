package energy.eddie.aiida.streamers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.dtos.ConnectionStatusMessage;
import energy.eddie.aiida.models.FailedToSendEntity;
import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.repositories.FailedToSendRepository;
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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MqttStreamerTest {
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
    private ConnectionStatusMessage mockStatusMessage;
    private final TestPublisher<AiidaRecord> recordPublisher = TestPublisher.create();
    private final Sinks.One<String> terminationSink = Sinks.one();
    private final AiidaRecord record1 = new AiidaRecord(Instant.now(), "Test", List.of(
            new AiidaRecordValue("1.8.0", "1.8.0", "444", "kWh", "10", "kWh")));
    private final AiidaRecord record2 = new AiidaRecord(Instant.now(),
                                                        "Test",
                                                        List.of(new AiidaRecordValue("2.8.0",
                                                                                     "2.8.0",
                                                                                     "888",
                                                                                     "kWh",
                                                                                     "10",
                                                                                     "kWh")));
    private MqttStreamingConfig mqttStreamingConfig;
    private static final String EXPECTED_DATA_TOPIC = "aiida/v1/permission-id/data";
    private static final String EXPECTED_STATUS_TOPIC = "aiida/v1/permission-id/status";
    private static final String EXPECTED_TERMINATION_TOPIC = "aiida/v1/permission-id/termination";
    private MqttStreamer streamer;

    @BeforeEach
    void setUp() {
        mqttStreamingConfig = new MqttStreamingConfig("permission-id",
                                                      "username",
                                                      "password",
                                                      "tcp://localhost:1883",
                                                      EXPECTED_DATA_TOPIC,
                                                      EXPECTED_STATUS_TOPIC,
                                                      EXPECTED_TERMINATION_TOPIC);
        when(mockClient.getPendingTokens()).thenReturn(new IMqttToken[]{});

        streamer = new MqttStreamer(new Permission(), recordPublisher.flux(),
                                    terminationSink,
                                    mqttStreamingConfig,
                                    mockClient,
                                    mockMapper,
                                    mockRepository);
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
        verify(mockClient).connect(
                argThat(options -> options.getUserName().equals(mqttStreamingConfig.username())
                                   && !options.isCleanStart()
                                   && options.isAutomaticReconnect()
                                   && new String(options.getPassword(),
                                                 StandardCharsets.UTF_8).equals(mqttStreamingConfig.password())));
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
        StepVerifier stepVerifier = StepVerifier.create(terminationSink.asMono())
                                                .expectComplete()
                                                .verifyLater();

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
        when(mockMessage.getPayload()).thenReturn("permission-id".getBytes(StandardCharsets.UTF_8));
        when(mockClient.disconnect(anyLong())).thenReturn(mockDisconnectToken);
        StepVerifier stepVerifier = StepVerifier.create(terminationSink.asMono())
                                                .expectNext("permission-id")
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
        when(mockMessage.getPayload()).thenReturn("NotTheExpectedPermissionId".getBytes(StandardCharsets.UTF_8));
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
        when(mockMessage.getPayload()).thenReturn("permission-id".getBytes(StandardCharsets.UTF_8));

        // When
        streamer.messageArrived(EXPECTED_TERMINATION_TOPIC, mockMessage);
        recordPublisher.next(record1, record2);
        Thread.sleep(200);

        // Then
        verify(mockClient, never()).publish(any(), any(), anyInt(), anyBoolean());
    }

    @Test
    @SuppressWarnings("java:S2925")
    void givenExceptionWhileSending_savesToRepository() throws MqttException, InterruptedException, JsonProcessingException {
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
    void verify_closeTerminally_publishesSynchronously_andDeletesFailedToSendMessages() throws MqttException, JsonProcessingException {
        // Given
        StepVerifier stepVerifier = StepVerifier.create(terminationSink.asMono())
                                                .expectComplete()
                                                .verifyLater();
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
        when(mockRepository.findAllByPermissionId("permission-id"))
                .thenReturn(List.of(new FailedToSendEntity("foo",
                                                           "bar",
                                                           "json".getBytes(StandardCharsets.UTF_8))));
        streamer.connect();

        // When
        streamer.connectComplete(false, "tcp://localhost:1883");

        // Then
        verify(mockRepository).deleteAllById(any());
        verify(mockClient, times(1)).publish(anyString(), any(), anyInt(), anyBoolean());
    }
}
