package energy.eddie.outbound.amqp;

import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Publisher;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_91_08.retransmission.ESMPDateTimeInterval;
import energy.eddie.cim.v0_91_08.retransmission.RTREnvelope;
import energy.eddie.outbound.shared.Endpoints;
import energy.eddie.outbound.shared.serde.MessageSerde;
import energy.eddie.outbound.shared.serde.SerializationException;
import energy.eddie.outbound.shared.serde.XmlMessageSerde;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AmqpInboundTest {
    private static final RabbitMQContainer rabbit =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:4-management-alpine"));
    private final MessageSerde serde = new XmlMessageSerde();
    private Connection connection;
    private AmqpInbound amqpInbound;

    AmqpInboundTest() throws Exception {}

    @BeforeAll
    static void setUpAll() {
        rabbit.start();
    }

    @AfterAll
    static void tearDownAll() {
        rabbit.stop();
    }

    @BeforeEach
    void setUp() {
        var connector = new AmqpOutboundConnector();
        connection = connector.connection(connector.amqpEnvironment(), rabbit.getAmqpUrl());
        amqpInbound = new AmqpInbound(connection, serde);
    }

    @AfterEach
    void tearDown() {
        amqpInbound.close();
        connection.close();
    }

    @Test
    void testTermination_acceptsMessage() throws InterruptedException, SerializationException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        var publisher = connection.publisherBuilder()
                                  .queue(Endpoints.V0_82.TERMINATIONS)
                                  .build();
        var msg = publisher.message(serde.serialize(new PermissionEnvelope()));
        // When
        publisher.publish(msg, ctx -> {
            assertEquals(Publisher.Status.ACCEPTED, ctx.status());
            latch.countDown();
        });

        // Then
        var res = latch.await(5, TimeUnit.SECONDS);
        assertTrue(res, "assertions in callback might have failed");
        var pair = amqpInbound.getTerminationMessages().blockFirst();
        assertNotNull(pair);

        // Clean-Up
        publisher.close();
    }

    @Test
    void testTermination_discardsInvalidMessage() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        var publisher = connection.publisherBuilder()
                                  .queue(Endpoints.V0_82.TERMINATIONS)
                                  .build();
        var msg = publisher.message("INVALID MESSAGE".getBytes(StandardCharsets.UTF_8));
        // When
        publisher.publish(msg, ctx -> {
            assertEquals(Publisher.Status.ACCEPTED, ctx.status());
            latch.countDown();
        });

        // Then
        var res = latch.await(5, TimeUnit.SECONDS);
        assertTrue(res, "assertions in callback might have failed");

        // Clean-Up
        publisher.close();
    }

    @Test
    void testRetransmissionRequests_acceptsMessage() throws InterruptedException, SerializationException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        var publisher = connection.publisherBuilder()
                                  .queue(Endpoints.V0_91_08.RETRANSMISSIONS)
                                  .build();
        var envelope = new RTREnvelope()
                .withMessageDocumentHeaderMetaInformationPermissionId("permissionId")
                .withMessageDocumentHeaderMetaInformationRegionConnector("rc-id")
                .withMarketDocumentPeriodTimeInterval(
                        new ESMPDateTimeInterval()
                                .withStart("2025-01-01T01:01Z")
                                .withEnd("2025-01-02T00:00Z")
                );
        var msg = publisher.message(serde.serialize(envelope));

        // When
        publisher.publish(msg, ctx -> {
            assertEquals(Publisher.Status.ACCEPTED, ctx.status());
            latch.countDown();
        });

        // Then
        var res = latch.await(5, TimeUnit.SECONDS);
        assertTrue(res, "assertions in callback might have failed");
        var pair = amqpInbound.retransmissionRequests().blockFirst();
        assertNotNull(pair);

        // Clean-Up
        publisher.close();
    }

    @Test
    void testRetransmissionRequests_discardsInvalidMessage() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        var publisher = connection.publisherBuilder()
                                  .queue(Endpoints.V0_91_08.RETRANSMISSIONS)
                                  .build();
        var msg = publisher.message("INVALID MESSAGE".getBytes(StandardCharsets.UTF_8));
        // When
        publisher.publish(msg, ctx -> {
            assertEquals(Publisher.Status.ACCEPTED, ctx.status());
            latch.countDown();
        });

        // Then
        var res = latch.await(5, TimeUnit.SECONDS);
        assertTrue(res, "assertions in callback might have failed");

        // Clean-Up
        publisher.close();
    }
}