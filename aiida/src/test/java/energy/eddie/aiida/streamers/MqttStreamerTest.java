package energy.eddie.aiida.streamers;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.dtos.ConnectionStatusMessage;
import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordFactory;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MqttStreamerTest {
    @Mock
    private MqttAsyncClient mockClient;
    @Mock
    private IMqttToken mockToken;
    @Mock
    private MqttMessage mockMessage;
    @Mock
    private FailedToSendRepository mockRepository;
    private final ObjectMapper mapper = new AiidaConfiguration().objectMapper();
    private final TestPublisher<AiidaRecord> recordPublisher = TestPublisher.create();
    private final TestPublisher<ConnectionStatusMessage> statusMessagePublisher = TestPublisher.create();
    private final Sinks.One<String> terminationSink = Sinks.one();
    private final AiidaRecord record1 = AiidaRecordFactory.createRecord("1.8.0", Instant.now(), 444);
    private final AiidaRecord record2 = AiidaRecordFactory.createRecord("2.8.0", Instant.now(), 888);
    private final ConnectionStatusMessage statusMessage = new ConnectionStatusMessage("foo",
                                                                                      "bar",
                                                                                      Instant.now(),
                                                                                      PermissionStatus.REJECTED,
                                                                                      "permission-id");
    private MqttStreamingConfig mqttStreamingConfig;
    private static final String expectedDataTopic = "aiida/v1/permission-id/data";
    private static final String expectedStatusTopic = "aiida/v1/permission-id/status";
    private static final String expectedTerminationTopic = "aiida/v1/permission-id/termination";
    private MqttStreamer streamer;

    @BeforeEach
    void setUp() {
        mqttStreamingConfig = MqttStreamingConfig.getFixedConfig("permission-id");

        streamer = new MqttStreamer(recordPublisher.flux(),
                                    statusMessagePublisher.flux(),
                                    terminationSink,
                                    mqttStreamingConfig,
                                    mockClient,
                                    mapper,
                                    mockRepository);

        when(mockClient.getPendingTokens()).thenReturn(new IMqttToken[]{});
    }


    @Test
    void verify_connect_setsCallback_andSubscribes() {
        // Given
        recordPublisher.assertNoSubscribers();
        statusMessagePublisher.assertNoSubscribers();

        // When
        streamer.connect();

        // Then
        verify(mockClient).setCallback(any());
        recordPublisher.assertSubscribers(1);
        statusMessagePublisher.assertSubscribers(1);
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
        verify(mockClient).subscribe(expectedTerminationTopic, 2);
    }

    @Test
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
        verify(mockClient).subscribe(expectedTerminationTopic, 2);
        verify(mockClient, times(2)).publish(eq(expectedDataTopic), any(), eq(1), eq(false));
    }

    @Test
    void givenStatusMessage_sendsViaMqtt() throws MqttException, InterruptedException {
        // Given
        streamer.connect();
        // manually call callback
        streamer.connectComplete(false, "fooTest");

        // When
        statusMessagePublisher.next(statusMessage);
        // Need to sleep because messages are handled on different thread and therefore a race condition may occur otherwise
        Thread.sleep(200);

        // Then
        verify(mockClient).setCallback(any());
        verify(mockClient).subscribe(expectedTerminationTopic, 2);
        verify(mockClient).publish(eq(expectedStatusTopic), any(), eq(1), eq(false));
    }

    @Test
    void verify_close_emitsEmptyOnTerminationOne_andClosesClient() throws MqttException {
        // Given
        when(mockClient.disconnect(anyLong())).thenReturn(mockToken);
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
        when(mockClient.disconnect(anyLong())).thenReturn(mockToken);
        StepVerifier stepVerifier = StepVerifier.create(terminationSink.asMono())
                                                .expectNext("permission-id")
                                                .then(streamer::close)
                                                .expectComplete()
                                                .verifyLater();
        streamer.connect();
        streamer.connectComplete(false, "fooTest");

        // When
        streamer.messageArrived(expectedTerminationTopic, mockMessage);

        // Then
        stepVerifier.verify(Duration.ofSeconds(2));
    }

    @Test
    void givenTerminationRequestWithInvalidPermissionId_doesNotPublishOnMono() throws MqttException {
        // Given
        when(mockMessage.getPayload()).thenReturn("NotTheExpectedPermissionId".getBytes(StandardCharsets.UTF_8));
        when(mockClient.disconnect(anyLong())).thenReturn(mockToken);
        StepVerifier stepVerifier = StepVerifier.create(terminationSink.asMono())
                                                .then(streamer::close)
                                                .expectComplete()
                                                .verifyLater();
        streamer.connect();
        streamer.connectComplete(false, "fooTest");

        // When
        streamer.messageArrived(expectedTerminationTopic, mockMessage);

        // Then
        stepVerifier.verify(Duration.ofSeconds(2));
    }

    @Test
    void givenAiidaRecordOrStatusMessageAfterTerminationRequest_doesNotSendViaMqtt() throws MqttException, InterruptedException {
        // Given
        streamer.connect();
        // manually call callback
        streamer.connectComplete(false, "fooTest");
        when(mockMessage.getPayload()).thenReturn("permission-id".getBytes(StandardCharsets.UTF_8));

        // When
        streamer.messageArrived(expectedTerminationTopic, mockMessage);
        statusMessagePublisher.next(statusMessage);
        recordPublisher.next(record1, record2);
        Thread.sleep(200);

        // Then
        verify(mockClient, never()).publish(any(), any(), anyInt(), anyBoolean());
    }

    // TODO test persistence when exception thrown
}
