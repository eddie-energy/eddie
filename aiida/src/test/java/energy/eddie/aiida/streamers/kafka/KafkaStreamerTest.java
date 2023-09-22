package energy.eddie.aiida.streamers.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.aiida.models.permission.KafkaStreamingConfig;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordFactory;
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
    private TestPublisher<AiidaRecord> publisher;
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
        publisher = TestPublisher.create();
        mockProducer = new MockProducer<>(true, new StringSerializer(), new StringSerializer());
        streamer = new KafkaStreamer(mockProducer, publisher.flux(), "RandomConnectionId", kafkaConfig, mapper);
    }

    @AfterEach
    void tearDown() {
        streamer.shutdown();
        mockProducer.close(Duration.ofMillis(100));
    }

    @Test
    void verify_connectAfterFluxPublished_doesNotSendAnyRecords() {
        // publishing records before KafkaStreamer subscribed to Flux, should mean that no records have been sent
        publisher.next(record1);
        publisher.next(record2);
        publisher.next(record3);

        publisher.assertNoSubscribers();
        // in connect function, the subscription is started
        streamer.connect();
        publisher.assertSubscribers(1);

        assertEquals(0, mockProducer.history().size());
    }

    @Test
    void verify_allRecordsPublishedByFlux_areSentByKafkaProducer() {
        publisher.assertNoSubscribers();
        streamer.connect();
        publisher.assertSubscribers(1);

        publisher.next(record1);
        publisher.next(record2);
        publisher.next(record3);

        assertEquals(3, mockProducer.history().size());
    }

    @Test
    void verify_jsonMappingFails_resultsInNoRecordsSent() throws JsonProcessingException {
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        streamer = new KafkaStreamer(mockProducer, publisher.flux(), "FooBarIdRandom", kafkaConfig, mockMapper);

        when(mockMapper.writeValueAsString(any())).thenThrow(JsonMappingException.class);

        streamer.connect();
        publisher.next(record1);

        assertEquals(0, mockProducer.history().size());
    }

    @Test
    void verify_shutdown_flushesClosesAndUnsubscribes() {
        // send some data before shutting down
        streamer.connect();
        publisher.assertSubscribers(1);
        publisher.next(record1);
        publisher.next(record2);
        publisher.next(record3);

        assertEquals(3, mockProducer.history().size());


        streamer.shutdown();

        publisher.assertNoSubscribers();
        assertTrue(mockProducer.closed());
        // apparently close calls flush() internally so all messages are sent to broker
        assertTrue(mockProducer.flushed());
    }

    @Test
    void verify_sendExceptions_areHandledInCallback() {
        mockProducer = new MockProducer<>(false, new StringSerializer(), new StringSerializer());
        streamer = new KafkaStreamer(mockProducer, publisher.flux(), "FooBarIdRandom", kafkaConfig, mapper);

        streamer.connect();

        publisher.next(record1);
        RuntimeException e = new SerializationException("expected by test");
        mockProducer.errorNext(e);

        Mockito.verify(logger).error(startsWith("Failed to send aiidaRecord "), any(AiidaRecord.class), any(SerializationException.class));
    }

    @Test
    void verify_shutdownWithoutPreviousConnect_doesntThrowNPE() {
        assertDoesNotThrow(() -> streamer.shutdown());
        assertTrue(mockProducer.closed());
    }

    @Test
    void verify_closedProducerThrowsIllegalStateException_isHandledByTryCatch() {
        mockProducer = new MockProducer<>(false, new StringSerializer(), new StringSerializer());
        streamer = new KafkaStreamer(mockProducer, publisher.flux(), "FooBarIdRandom", kafkaConfig, mapper);

        streamer.connect();
        mockProducer.close();
        publisher.next(record1);

        Mockito.verify(logger).error(startsWith("Error while sending aiidaRecord "), any(AiidaRecord.class), any(IllegalStateException.class));
    }

    @Test
    void verify_exceptionDuringShutdown_isHandledByTryCatch() throws KafkaException {
        streamer = new KafkaStreamer(mockitoMock, publisher.flux(), "RandomConnectionId", kafkaConfig, mapper);

        // mockProducer has a closeException that would be thrown when it is closed,
        // but using it will always result in a failed test case, therefore use a Mockito mock
        doThrow(new KafkaException("Expected by test case"))
                .when(mockitoMock).close();

        streamer.connect();
        publisher.next(record1);

        streamer.shutdown();
        Mockito.verify(logger).error(startsWith("Error while shutting down KafkaStreamer for connectionId "),
                anyString(), any(KafkaException.class));
    }
}
