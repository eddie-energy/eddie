// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.amqp;

import com.rabbitmq.client.amqp.Connection;
import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.pmd.MessageDocumentHeaderComplexType;
import energy.eddie.cim.v0_82.pmd.MessageDocumentHeaderMetaInformationComplexType;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.outbound.shared.Headers;
import energy.eddie.outbound.shared.TopicConfiguration;
import energy.eddie.outbound.shared.TopicStructure;
import energy.eddie.outbound.shared.testing.MockDataSourceInformation;
import org.junit.jupiter.api.*;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;
import reactor.test.publisher.TestPublisher;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DirtiesContext
class AmqpOutboundTest {
    private static final RabbitMQContainer rabbit = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:4-management-alpine")
    );
    private final MockDataSourceInformation dataSourceInformation = new MockDataSourceInformation("AT",
                                                                                                  "at-eda",
                                                                                                  "paid",
                                                                                                  "mdaid");
    private final TopicConfiguration config = new TopicConfiguration("eddie");
    private Connection connection;
    private AmqpOutbound amqpOutbound;

    @BeforeAll
    static void setUpAll() {
        rabbit.start();
    }

    @AfterAll
    static void tearDownAll() {
        rabbit.stop();
    }

    @BeforeEach
    void setUp() throws Exception {
        var connector = new AmqpOutboundConnector();
        connection = connector.connection(connector.amqpEnvironment(),
                                          rabbit.getAmqpUrl(),
                                          config);
        amqpOutbound = new AmqpOutbound(connection, new XmlMessageSerde(), config);
    }

    @AfterEach
    void tearDown() {
        amqpOutbound.close();
        connection.close();
    }

    @Test
    void testConnectionStatusMessages_producesMessage() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        TestPublisher<ConnectionStatusMessage> csmPublisher = TestPublisher.create();
        amqpOutbound.setConnectionStatusMessageStream(csmPublisher.flux());
        var connectionStatusMessage = new ConnectionStatusMessage("cid",
                                                                  "pid",
                                                                  "dnid",
                                                                  dataSourceInformation,
                                                                  PermissionProcessStatus.ACCEPTED);
        // When
        csmPublisher.emit(connectionStatusMessage);

        // Then
        var consumer = connection.consumerBuilder()
                                 .queue(config.connectionStatusMessage())
                                 .messageHandler((ctx, msg) -> {
                                     assertAll(
                                             () -> assertEquals("pid", msg.property(Headers.PERMISSION_ID)),
                                             () -> assertEquals("cid", msg.property(Headers.CONNECTION_ID)),
                                             () -> assertEquals("dnid", msg.property(Headers.DATA_NEED_ID))
                                     );
                                     latch.countDown();
                                 })
                                 .build();
        var res = latch.await(5, TimeUnit.SECONDS);
        assertTrue(res, "Assertions in message handler might have failed");

        // Clean-Up
        consumer.close();
        csmPublisher.complete();
    }

    @Test
    void testRawDataMessages_producesMessage() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        TestPublisher<RawDataMessage> publisher = TestPublisher.create();
        amqpOutbound.setRawDataStream(publisher.flux());
        var message = new RawDataMessage("pid",
                                         "cid",
                                         "dnid",
                                         dataSourceInformation,
                                         ZonedDateTime.now(ZoneOffset.UTC),
                                         "");
        // When
        publisher.emit(message);

        // Then
        var consumer = connection.consumerBuilder()
                                 .queue(config.rawDataMessage())
                                 .messageHandler((ctx, msg) -> {
                                     assertAll(
                                             () -> assertEquals("pid", msg.property(Headers.PERMISSION_ID)),
                                             () -> assertEquals("cid", msg.property(Headers.CONNECTION_ID)),
                                             () -> assertEquals("dnid", msg.property(Headers.DATA_NEED_ID))
                                     );
                                     latch.countDown();
                                 })
                                 .build();
        var res = latch.await(5, TimeUnit.SECONDS);
        assertTrue(res, "Assertions in message handler might have failed");

        // Clean-Up
        consumer.close();
        publisher.complete();
    }

    @Test
    void testPermissionMarketDocuments_producesMessage() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        TestPublisher<PermissionEnvelope> publisher = TestPublisher.create();
        amqpOutbound.setPermissionMarketDocumentStream(publisher.flux());
        var message = new PermissionEnvelope()
                .withMessageDocumentHeader(
                        new MessageDocumentHeaderComplexType()
                                .withMessageDocumentHeaderMetaInformation(
                                        new MessageDocumentHeaderMetaInformationComplexType()
                                                .withPermissionid("pid")
                                                .withConnectionid("cid")
                                                .withDataNeedid("dnid")
                                )
                );
        // When
        publisher.emit(message);

        // Then
        var consumer = connection.consumerBuilder()
                                 .queue(config.permissionMarketDocument())
                                 .messageHandler((ctx, msg) -> {
                                     assertAll(
                                             () -> assertEquals("pid", msg.property(Headers.PERMISSION_ID)),
                                             () -> assertEquals("cid", msg.property(Headers.CONNECTION_ID)),
                                             () -> assertEquals("dnid", msg.property(Headers.DATA_NEED_ID))
                                     );
                                     latch.countDown();
                                 })
                                 .build();
        var res = latch.await(5, TimeUnit.SECONDS);
        assertTrue(res, "Assertions in message handler might have failed");

        // Clean-Up
        consumer.close();
        publisher.complete();
    }

    @Test
    void testAccountingPointMarketDocuments_producesMessage() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        TestPublisher<AccountingPointEnvelope> publisher = TestPublisher.create();
        amqpOutbound.setAccountingPointEnvelopeStream(publisher.flux());
        var message = new AccountingPointEnvelope()
                .withMessageDocumentHeader(
                        new energy.eddie.cim.v0_82.ap.MessageDocumentHeaderComplexType()
                                .withMessageDocumentHeaderMetaInformation(
                                        new energy.eddie.cim.v0_82.ap.MessageDocumentHeaderMetaInformationComplexType()
                                                .withPermissionid("pid")
                                                .withConnectionid("cid")
                                                .withDataNeedid("dnid")
                                )
                );
        // When
        publisher.emit(message);

        // Then
        var consumer = connection.consumerBuilder()
                                 .queue(config.accountingPointMarketDocument())
                                 .messageHandler((ctx, msg) -> {
                                     assertAll(
                                             () -> assertEquals("pid", msg.property(Headers.PERMISSION_ID)),
                                             () -> assertEquals("cid", msg.property(Headers.CONNECTION_ID)),
                                             () -> assertEquals("dnid", msg.property(Headers.DATA_NEED_ID))
                                     );
                                     latch.countDown();
                                 })
                                 .build();
        var res = latch.await(5, TimeUnit.SECONDS);
        assertTrue(res, "Assertions in message handler might have failed");

        // Clean-Up
        consumer.close();
        publisher.complete();
    }

    @Test
    void testValidatedHistoricalData_producesMessage() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        TestPublisher<ValidatedHistoricalDataEnvelope> publisher = TestPublisher.create();
        amqpOutbound.setEddieValidatedHistoricalDataMarketDocumentStream(publisher.flux());
        var message = new ValidatedHistoricalDataEnvelope()
                .withMessageDocumentHeader(
                        new energy.eddie.cim.v0_82.vhd.MessageDocumentHeaderComplexType()
                                .withMessageDocumentHeaderMetaInformation(
                                        new energy.eddie.cim.v0_82.vhd.MessageDocumentHeaderMetaInformationComplexType()
                                                .withPermissionid("pid")
                                                .withConnectionid("cid")
                                                .withDataNeedid("dnid")
                                )
                );
        // When
        publisher.emit(message);

        // Then
        var consumer = connection.consumerBuilder()
                                 .queue(config.validatedHistoricalDataMarketDocument(TopicStructure.DataModels.CIM_0_82))
                                 .messageHandler((ctx, msg) -> {
                                     assertAll(
                                             () -> assertEquals("pid", msg.property(Headers.PERMISSION_ID)),
                                             () -> assertEquals("cid", msg.property(Headers.CONNECTION_ID)),
                                             () -> assertEquals("dnid", msg.property(Headers.DATA_NEED_ID))
                                     );
                                     latch.countDown();
                                 })
                                 .build();
        var res = latch.await(5, TimeUnit.SECONDS);
        assertTrue(res, "Assertions in message handler might have failed");

        // Clean-Up
        consumer.close();
        publisher.complete();
    }

    @Test
    void testNearRealTimeDataCimV1_04_producesMessage() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        TestPublisher<energy.eddie.cim.v1_04.rtd.RTDEnvelope> publisher = TestPublisher.create();
        amqpOutbound.setNearRealTimeDataMarketDocumentStreamV1_04(publisher.flux());
        var message = new energy.eddie.cim.v1_04.rtd.RTDEnvelope()
                .withMessageDocumentHeaderMetaInformationPermissionId("pid")
                .withMessageDocumentHeaderMetaInformationConnectionId("cid")
                .withMessageDocumentHeaderMetaInformationDataNeedId("dnid");

        // When
        publisher.emit(message);

        // Then
        var consumer = connection.consumerBuilder()
                                 .queue(config.nearRealTimeDataMarketDocument(TopicStructure.DataModels.CIM_1_04))
                                 .messageHandler((ctx, msg) -> {
                                     assertAll(
                                             () -> assertEquals("pid", msg.property(Headers.PERMISSION_ID)),
                                             () -> assertEquals("cid", msg.property(Headers.CONNECTION_ID)),
                                             () -> assertEquals("dnid", msg.property(Headers.DATA_NEED_ID))
                                     );
                                     latch.countDown();
                                 })
                                 .build();
        var res = latch.await(5, TimeUnit.SECONDS);
        assertTrue(res, "Assertions in message handler might have failed");

        // Clean-Up
        consumer.close();
        publisher.complete();
    }

    @Test
    void testNearRealTimeDataCimV1_12_producesMessage() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        TestPublisher<energy.eddie.cim.v1_12.rtd.RTDEnvelope> publisher = TestPublisher.create();
        amqpOutbound.setNearRealTimeDataMarketDocumentStreamV1_12(publisher.flux());
        var metaInformation = new energy.eddie.cim.v1_12.rtd.MetaInformation()
                .withRequestPermissionId("pid")
                .withConnectionId("cid")
                .withDataNeedId("dnid");
        var header = new energy.eddie.cim.v1_12.rtd.MessageDocumentHeader()
                .withMetaInformation(metaInformation);
        var message = new energy.eddie.cim.v1_12.rtd.RTDEnvelope()
                .withMessageDocumentHeader(header);

        // When
        publisher.emit(message);

        // Then
        var consumer = connection.consumerBuilder()
                                 .queue(config.nearRealTimeDataMarketDocument(TopicStructure.DataModels.CIM_1_12))
                                 .messageHandler((ctx, msg) -> {
                                     assertAll(
                                             () -> assertEquals("pid", msg.property(Headers.PERMISSION_ID)),
                                             () -> assertEquals("cid", msg.property(Headers.CONNECTION_ID)),
                                             () -> assertEquals("dnid", msg.property(Headers.DATA_NEED_ID))
                                     );
                                     latch.countDown();
                                 })
                                 .build();
        var res = latch.await(5, TimeUnit.SECONDS);
        assertTrue(res, "Assertions in message handler might have failed");

        // Clean-Up
        consumer.close();
        publisher.complete();
    }

    @Test
    void testValidatedHistoricalDataMarketDocumentsV1_04_producesMessage() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        TestPublisher<VHDEnvelope> publisher = TestPublisher.create();
        amqpOutbound.setValidatedHistoricalDataMarketDocumentStream(publisher.flux());
        var message = new VHDEnvelope()
                .withMessageDocumentHeaderMetaInformationPermissionId("pid")
                .withMessageDocumentHeaderMetaInformationConnectionId("cid")
                .withMessageDocumentHeaderMetaInformationDataNeedId("dnid");

        // When
        publisher.emit(message);

        // Then
        var consumer = connection.consumerBuilder()
                                 .queue(config.validatedHistoricalDataMarketDocument(TopicStructure.DataModels.CIM_1_04))
                                 .messageHandler((ctx, msg) -> {
                                     assertAll(
                                             () -> assertEquals("pid", msg.property(Headers.PERMISSION_ID)),
                                             () -> assertEquals("cid", msg.property(Headers.CONNECTION_ID)),
                                             () -> assertEquals("dnid", msg.property(Headers.DATA_NEED_ID))
                                     );
                                     latch.countDown();
                                 })
                                 .build();
        var res = latch.await(5, TimeUnit.SECONDS);
        assertTrue(res, "Assertions in message handler might have failed");

        // Clean-Up
        consumer.close();
        publisher.complete();
    }
}