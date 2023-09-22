package energy.eddie.aiida.streamers.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.aiida.models.permission.KafkaStreamingConfig;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordFactory;
import energy.eddie.aiida.streamers.ConnectionStatusMessage;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaStreamerTest {
    @Mock
    Logger logger;
    // use @Mock annotation to get a typed mock
    @Mock
    KafkaProducer<String, String> mockitoMock;
    private KafkaStreamingConfig kafkaConfig;
    private AiidaRecord record1;
    private AiidaRecord record2;
    private AiidaRecord record3;
    private ObjectMapper mapper;
    private TestPublisher<AiidaRecord> recordPublisher;
    private TestPublisher<ConnectionStatusMessage> statusMessagePublisher;
    private MockProducer<String, String> mockProducer;
    @InjectMocks
    private KafkaStreamer streamer;

    @BeforeEach
    void setUp() {
        String bootstrapServers = "localhost:9092";
        String validDataTopic = "ValidPublishTopic";
        String validStatusTopic = "ValidStatusTopic";
        String validSubscribeTopic = "ValidSubscribeTopic";
        kafkaConfig = new KafkaStreamingConfig(bootstrapServers, validDataTopic, validStatusTopic, validSubscribeTopic);

        var now = Instant.now();
        record1 = AiidaRecordFactory.createRecord("1.8.0", now.plusSeconds(1), 10);
        record2 = AiidaRecordFactory.createRecord("1.8.0", now.plusSeconds(2), 20);
        record3 = AiidaRecordFactory.createRecord("1.8.0", now.plusSeconds(3), 30);

        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        recordPublisher = TestPublisher.create();
        statusMessagePublisher = TestPublisher.create();
        mockProducer = new MockProducer<>(true, new StringSerializer(), new StringSerializer());
        streamer = new KafkaStreamer(mockProducer, recordPublisher.flux(), statusMessagePublisher.flux(),
                "RandomConnectionId", kafkaConfig, mapper);
    }

    @AfterEach
    void tearDown() {
        streamer.close();
        mockProducer.close(Duration.ofMillis(100));
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
        streamer = new KafkaStreamer(mockProducer, recordPublisher.flux(), statusMessagePublisher.flux(),
                "FooBarIdRandom", kafkaConfig, mockMapper);

        when(mockMapper.writeValueAsString(any())).thenThrow(JsonMappingException.class);

        streamer.connect();
        recordPublisher.next(record1);

        assertEquals(0, mockProducer.history().size());
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
        assertTrue(mockProducer.closed());
        // apparently close calls flush() internally so all messages are sent to broker
        assertTrue(mockProducer.flushed());
    }

    @Test
    void verify_sendExceptions_areHandledInCallback() {
        var connectionId = "FooBarIdRandom";
        var statusMessage1 = new ConnectionStatusMessage(connectionId, Instant.now(), PermissionStatus.ACCEPTED);

        mockProducer = new MockProducer<>(false, new StringSerializer(), new StringSerializer());
        streamer = new KafkaStreamer(mockProducer, recordPublisher.flux(), statusMessagePublisher.flux(),
                connectionId, kafkaConfig, mapper);

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
        streamer = new KafkaStreamer(mockProducer, recordPublisher.flux(), statusMessagePublisher.flux(),
                connectionId, kafkaConfig, mapper);

        streamer.connect();
        mockProducer.close();

        recordPublisher.next(record1);
        Mockito.verify(logger).error(startsWith("Error while sending data "), any(AiidaRecord.class), any(IllegalStateException.class));


        statusMessagePublisher.next(statusMessage1);
        Mockito.verify(logger).error(startsWith("Error while sending data "), any(ConnectionStatusMessage.class), any(IllegalStateException.class));
    }

    @Test
    void verify_exceptionDuringShutdown_isHandledByTryCatch() throws KafkaException {
        streamer = new KafkaStreamer(mockitoMock, recordPublisher.flux(), statusMessagePublisher.flux(),
                "RandomConnectionId", kafkaConfig, mapper);

        // mockProducer has a closeException that would be thrown when it is closed,
        // but using it will always result in a failed test case, therefore use a Mockito mock
        doThrow(new KafkaException("Expected by test case"))
                .when(mockitoMock).close();

        streamer.connect();
        recordPublisher.next(record1);

        streamer.close();
        Mockito.verify(logger).error(startsWith("Error while shutting down KafkaStreamer for connectionId "),
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
}
