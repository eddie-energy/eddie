package energy.eddie.aiida.streamers.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.aiida.models.permission.KafkaStreamingConfig;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordFactory;
import energy.eddie.aiida.streamers.ConnectionStatusMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaStreamerTest {
    @Mock(name = "energy.eddie.aiida.streamers.kafka.KafkaStreamer")
    private Logger logger;
    // use @Mock annotation instead of mock(...) method to get a typed mock
    @Mock
    private KafkaProducer<String, String> mockitoMock;
    @Mock
    private TaskScheduler scheduler;
    private KafkaStreamer streamer;
    private KafkaStreamingConfig kafkaConfig;
    private AiidaRecord record1;
    private AiidaRecord record2;
    private AiidaRecord record3;
    private ObjectMapper mapper;
    private TestPublisher<AiidaRecord> recordPublisher;
    private TestPublisher<ConnectionStatusMessage> statusMessagePublisher;
    private MockProducer<String, String> mockProducer;
    private MockConsumer<String, String> mockConsumer;
    private Sinks.One<String> terminationRequestSink;
    private Permission permission;

    @BeforeEach
    void setUp() {
        var permissionId = "040d3de7-5dee-49ca-95a8-8550505ed06f";
        var start = Instant.now();
        var expiration = start.plusSeconds(10000);
        String bootstrapServers = "localhost:9092";
        String validDataTopic = "ValidPublishTopic";
        String validStatusTopic = "ValidStatusTopic";
        String validSubscribeTopic = "ValidSubscribeTopic";
        kafkaConfig = new KafkaStreamingConfig(bootstrapServers, validDataTopic, validStatusTopic, validSubscribeTopic);
        permission = new Permission("SomeTest Service Name", start, expiration, start, "ConnId",
                Set.of("1.8.0"), kafkaConfig);
        ReflectionTestUtils.setField(permission, "permissionId", permissionId);

        var now = Instant.now();
        record1 = AiidaRecordFactory.createRecord("1.8.0", now.plusSeconds(1), 10);
        record2 = AiidaRecordFactory.createRecord("1.8.0", now.plusSeconds(2), 20);
        record3 = AiidaRecordFactory.createRecord("1.8.0", now.plusSeconds(3), 30);

        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        recordPublisher = TestPublisher.create();
        statusMessagePublisher = TestPublisher.create();
        mockProducer = new MockProducer<>(true, new StringSerializer(), new StringSerializer());
        mockConsumer = new MockConsumer<>(OffsetResetStrategy.LATEST);
        terminationRequestSink = Sinks.one();
        streamer = new KafkaStreamer(mockProducer, mockConsumer, recordPublisher.flux(), statusMessagePublisher.flux(),
                terminationRequestSink, permission, mapper, scheduler, Duration.ofSeconds(10));
    }

    @AfterEach
    void tearDown() {
        streamer.close();
        mockProducer.close(Duration.ofMillis(10));
        mockConsumer.close(Duration.ofMillis(10));
    }

    @Test
    void givenInvalidPollDuration_constructor_throws() {
        var mockScheduler = mock(TaskScheduler.class);
        var recordFlux = recordPublisher.flux();
        var statusFlux = statusMessagePublisher.flux();
        var duration = Duration.ofSeconds(5);
        var thrown = assertThrows(IllegalArgumentException.class, () ->
                new KafkaStreamer(mockProducer, mockConsumer, recordFlux, statusFlux,
                        terminationRequestSink, permission, mapper, mockScheduler, duration));

        assertEquals("terminationRequestPollDuration must be greater or equal to 10 seconds", thrown.getMessage());
    }

    @Test
    void verify_connectAfterFluxPublished_doesNotSendAnyRecords() {
        // publishing records before KafkaStreamer subscribed to Flux, should mean that no records have been sent
        recordPublisher.next(record1);
        recordPublisher.next(record2);
        recordPublisher.next(record3);

        recordPublisher.assertNoSubscribers();
        statusMessagePublisher.assertNoSubscribers();
        // in connect function, the subscription is started
        streamer.connect();
        recordPublisher.assertSubscribers(1);
        statusMessagePublisher.assertSubscribers(1);

        assertEquals(0, mockProducer.history().size());
    }

    @Test
    void verify_allRecordsPublishedByFlux_areSentByKafkaProducer() {
        recordPublisher.assertNoSubscribers();
        statusMessagePublisher.assertNoSubscribers();
        streamer.connect();
        statusMessagePublisher.assertSubscribers(1);
        recordPublisher.assertSubscribers(1);

        recordPublisher.next(record1);
        recordPublisher.next(record2);
        recordPublisher.next(record3);

        assertEquals(3, mockProducer.history().size());

        // validate they are sent to correct topic
        assertEquals(kafkaConfig.dataTopic(), mockProducer.history().get(0).topic());
        assertEquals(kafkaConfig.dataTopic(), mockProducer.history().get(1).topic());
        assertEquals(kafkaConfig.dataTopic(), mockProducer.history().get(2).topic());
    }

    @Test
    void verify_jsonMappingFails_resultsInNoRecordsSent() throws JsonProcessingException {
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        streamer = new KafkaStreamer(mockProducer, mockConsumer, recordPublisher.flux(), statusMessagePublisher.flux(),
                terminationRequestSink, permission, mockMapper, mock(TaskScheduler.class), Duration.ofSeconds(10));

        when(mockMapper.writeValueAsString(any())).thenThrow(JsonMappingException.class);

        streamer.connect();
        recordPublisher.next(record1);

        assertEquals(0, mockProducer.history().size());
    }

    @Test
    void givenTerminationRequestReceived_noRecordsAreSent() {
        ReflectionTestUtils.setField(streamer, "receivedTerminationRequest", true);

        streamer.connect();

        recordPublisher.next(record1);
        recordPublisher.next(record2);
        recordPublisher.next(record3);

        assertEquals(0, mockProducer.history().size());

        verify(logger, times(3)).debug("Got new aiidaRecord but won't send it as a termination request has been received.");
    }

    @Test
    void verify_shutdown_flushesClosesAndUnsubscribes() {
        // send some data before shutting down
        streamer.connect();
        recordPublisher.assertSubscribers(1);
        statusMessagePublisher.assertSubscribers(1);

        recordPublisher.next(record1);
        recordPublisher.next(record2);
        recordPublisher.next(record3);

        assertEquals(3, mockProducer.history().size());


        streamer.close();

        recordPublisher.assertNoSubscribers();
        statusMessagePublisher.assertNoSubscribers();
        assertEquals(0, terminationRequestSink.currentSubscriberCount());

        assertTrue(mockProducer.closed());
        // apparently close calls flush() internally so all messages are sent to broker
        assertTrue(mockProducer.flushed());
    }

    @Test
    void verify_sendExceptions_areHandledInCallback() {
        var connectionId = "FooBarIdRandom";
        var statusMessage1 = new ConnectionStatusMessage(connectionId, Instant.now(), PermissionStatus.ACCEPTED);

        mockProducer = new MockProducer<>(false, new StringSerializer(), new StringSerializer());
        streamer = new KafkaStreamer(mockProducer, mockConsumer, recordPublisher.flux(), statusMessagePublisher.flux(),
                terminationRequestSink, permission, mapper, mock(TaskScheduler.class), Duration.ofSeconds(10));

        streamer.connect();

        recordPublisher.next(record1);
        RuntimeException e = new SerializationException("expected by test");
        mockProducer.errorNext(e);

        Mockito.verify(logger).error(startsWith("Failed to send data "), any(AiidaRecord.class), any(SerializationException.class));


        statusMessagePublisher.next(statusMessage1);
        mockProducer.errorNext(e);

        Mockito.verify(logger).error(startsWith("Failed to send data "), any(ConnectionStatusMessage.class), any(SerializationException.class));
    }

    @Test
    void verify_shutdownWithoutPreviousConnect_doesntThrowNPE() {
        assertDoesNotThrow(() -> streamer.close());
        assertTrue(mockProducer.closed());
    }

    @Test
    void verify_closedProducerThrowsIllegalStateException_isHandledByTryCatch() {
        var connectionId = "FooBarIdRandom";
        var statusMessage1 = new ConnectionStatusMessage(connectionId, Instant.now(), PermissionStatus.ACCEPTED);

        mockProducer = new MockProducer<>(false, new StringSerializer(), new StringSerializer());
        streamer = new KafkaStreamer(mockProducer, mockConsumer, recordPublisher.flux(), statusMessagePublisher.flux(),
                terminationRequestSink, permission, mapper, mock(TaskScheduler.class), Duration.ofSeconds(10));

        streamer.connect();
        mockProducer.close();

        recordPublisher.next(record1);
        Mockito.verify(logger).error(startsWith("Error while sending data "), any(AiidaRecord.class), any(IllegalStateException.class));


        statusMessagePublisher.next(statusMessage1);
        Mockito.verify(logger).error(startsWith("Error while sending data "), any(ConnectionStatusMessage.class), any(IllegalStateException.class));
    }

    @Test
    void verify_exceptionDuringShutdown_isHandledByTryCatch() throws KafkaException {
        streamer = new KafkaStreamer(mockitoMock, mockConsumer, recordPublisher.flux(), statusMessagePublisher.flux(),
                terminationRequestSink, permission, mapper, mock(TaskScheduler.class), Duration.ofSeconds(10));

        // mockProducer has a closeException that would be thrown when it is closed,
        // but using it will always result in a failed test case, therefore use a Mockito mock
        doThrow(new KafkaException("Expected by test case"))
                .when(mockitoMock).close();

        streamer.connect();
        recordPublisher.next(record1);

        streamer.close();
        Mockito.verify(logger).error(eq("Error while shutting down KafkaStreamer for permission {}"),
                anyString(), any(KafkaException.class));
    }

    @Test
    void verify_statusMessage_areSentByKafkaProducer() {
        var connectionId = "MyStatusTestConnectionId";
        var statusMessage1 = new ConnectionStatusMessage(connectionId, Instant.now(), PermissionStatus.ACCEPTED);
        var statusMessage2 = new ConnectionStatusMessage(connectionId, Instant.now(), PermissionStatus.REVOKED);

        recordPublisher.assertNoSubscribers();
        statusMessagePublisher.assertNoSubscribers();

        streamer.connect();

        recordPublisher.assertSubscribers(1);
        statusMessagePublisher.assertSubscribers(1);


        statusMessagePublisher.next(statusMessage1);
        statusMessagePublisher.next(statusMessage2);

        assertEquals(2, mockProducer.history().size());
        assertEquals(kafkaConfig.statusTopic(), mockProducer.history().get(0).topic());
        assertEquals(kafkaConfig.statusTopic(), mockProducer.history().get(1).topic());


        streamer.close();
        recordPublisher.assertNoSubscribers();
        statusMessagePublisher.assertNoSubscribers();
    }

    /**
     * Tests that
     * <li>the consumer subscribes to the correct topic</li>
     * <li>when a termination request is received, no more {@link AiidaRecord}s are sent</li>
     * <li>when a termination request is received, the {@code permissionId} is published on the {@code terminationRequestMono}</li>
     */
    @Test
    @Timeout(5)
    void verify_TerminationRequest_isHandledAsExpected() {
        try (var scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()) {
            prepareTerminationRequestTestCase(scheduledExecutorService);

            // a record will be added to the consumer after the preparation work is done and the KafkaStreamer polls
            mockConsumer.schedulePollTask(() -> mockConsumer.addRecord(
                    new ConsumerRecord<>(permission.kafkaStreamingConfig().subscribeTopic(),
                            0,
                            0,
                            permission.connectionId(), permission.connectionId())));

            streamer.connect();

            // test that subscribed to correct topic
            Set<String> subscriptions = mockConsumer.subscription();
            assertEquals(1, subscriptions.size());
            assertEquals(permission.kafkaStreamingConfig().subscribeTopic(), subscriptions.iterator().next());

            // before termination request, data is successfully sent to EP
            recordPublisher.next(record1);
            assertEquals(1, mockProducer.history().size());

            // StepVerifier will block until it's conditions are fulfilled
            StepVerifier.create(terminationRequestSink.asMono())
                    .expectNext(permission.permissionId())
                    .verifyComplete();


            // after termination request is received, no more data should be sent
            recordPublisher.next(record2);
            recordPublisher.next(record3);
            assertEquals(1, mockProducer.history().size());
            verify(logger, times(2)).debug("Got new aiidaRecord but won't send it as a termination request has been received.");
        }
    }

    @Test
    @Timeout(5)
    void givenTerminationRequestWithWrongConnectionId_isLogged() throws InterruptedException {
        try (var scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()) {
            prepareTerminationRequestTestCase(scheduledExecutorService);
            var latch = new CountDownLatch(1);

            final String invalidConnectionId = "This is a wrong connectionId";
            mockConsumer.schedulePollTask(() -> mockConsumer.addRecord(
                    new ConsumerRecord<>(permission.kafkaStreamingConfig().subscribeTopic(),
                            0,
                            0,
                            "fooo", invalidConnectionId)));
            // after invalid request has been processed, continue with test case
            mockConsumer.schedulePollTask(latch::countDown);

            streamer.connect();
            latch.await();

            streamer.close();

            /* This verification fails, because somehow, when using the logger mock, the WARN log message is not logged
               and therefore, the verify(logger) call fails. However, when running the test without the logger mock, the
               WARN log message is correctly output to the console. Test coverage also shows, that the line is covered.
               I assume that this is because the WARN log message is produced in another thread and the
               slf4j2-mock implementation fails to properly mock it?
             */
            //verify(logger).warn("Got request from EP to terminate permission {} but they supplied wrong connectionId. Expected {}, but got {}",
            //        permission.permissionId(), permission.connectionId(), invalidConnectionId);

            // instead verify the side effect, that no permissionId is published on the Mono
            StepVerifier.create(terminationRequestSink.asMono())
                    .verifyComplete();
        }
    }

    private void prepareTerminationRequestTestCase(ScheduledExecutorService scheduledExecutorService) {
        streamer = new KafkaStreamer(mockProducer, mockConsumer, recordPublisher.flux(), statusMessagePublisher.flux(),
                terminationRequestSink, permission, mapper, scheduler, Duration.ofSeconds(10));

        // prepare mockConsumer so a termination request will be simulated when consumer is polled
        String topic = permission.kafkaStreamingConfig().subscribeTopic();
        TopicPartition topicPartition = new TopicPartition(topic, 0);
        Map<TopicPartition, Long> endOffsets = new HashMap<>();
        endOffsets.put(topicPartition, 0L);
        mockConsumer.schedulePollTask(() -> {
            // each poll will execute a new line
            mockConsumer.updateEndOffsets(endOffsets);
            mockConsumer.rebalance(Collections.singletonList(topicPartition));
        });

        // run scheduled task with higher frequency
        when(scheduler.scheduleAtFixedRate(any(), any(Duration.class))).thenAnswer(i ->
                scheduledExecutorService.scheduleAtFixedRate(() ->
                        ((Runnable) i.getArgument(0)).run(), 0, 100, TimeUnit.MILLISECONDS));
    }
}
