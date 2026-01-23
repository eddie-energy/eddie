// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.kafka;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.pmd.MessageDocumentHeaderComplexType;
import energy.eddie.cim.v0_82.pmd.MessageDocumentHeaderMetaInformationComplexType;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.outbound.shared.testing.MockDataSourceInformation;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import reactor.core.publisher.Flux;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {KafkaConnector.class, KafkaTestConfig.class})
@EmbeddedKafka
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Tag("Integration")
class KafkaConnectorTest {
    private final DataSourceInformation dataSourceInformation = new MockDataSourceInformation("AT",
                                                                                              "at-eda",
                                                                                              "paid",
                                                                                              "mdaid");
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;
    @Autowired
    private KafkaConnector kafkaConnector;

    @Test
    void testConnectionStatusMessages_areProducedToKafka() {
        // Given
        var csm = new ConnectionStatusMessage("cid",
                                              "pid",
                                              "dnid",
                                              dataSourceInformation,
                                              PermissionProcessStatus.ACCEPTED);
        kafkaConnector.setConnectionStatusMessageStream(Flux.just(csm));
        var consumerProps = KafkaTestUtils.consumerProps(embeddedKafka, "testGroup", true);
        var consumer = new DefaultKafkaConsumerFactory<String, Object>(consumerProps).createConsumer();
        consumer.subscribe(Collections.singleton("ep.eddie.agnostic.connection-status-message"));

        // When
        var records = KafkaTestUtils.getRecords(consumer);

        // Then
        assertThat(records).hasSize(1);
    }

    @Test
    void testPermissionMarketDocuments_areProducedToKafka() {
        // Given
        var data = new PermissionEnvelope()
                .withMessageDocumentHeader(
                        new MessageDocumentHeaderComplexType()
                                .withMessageDocumentHeaderMetaInformation(
                                        new MessageDocumentHeaderMetaInformationComplexType()
                                                .withPermissionid("pid")
                                                .withConnectionid("cid")
                                                .withDataNeedid("dnid")
                                                .withDataType("validated-historical-data-market-document")
                                )
                );
        kafkaConnector.setPermissionMarketDocumentStream(Flux.just(data));
        var consumerProps = KafkaTestUtils.consumerProps(embeddedKafka, "testGroup", true);
        var consumer = new DefaultKafkaConsumerFactory<>(consumerProps,
                                                         new StringDeserializer(),
                                                         new StringDeserializer()).createConsumer();
        consumer.subscribe(Collections.singleton("ep.eddie.cim_0_82.permission-md"));

        // When
        var records = KafkaTestUtils.getRecords(consumer);

        // Then
        assertThat(records).hasSize(1);
    }

    @Test
    void testValidatedHistoricalDataMarketDocuments_areProducedToKafka() {
        // Given
        var data = new ValidatedHistoricalDataEnvelope()
                .withMessageDocumentHeader(
                        new energy.eddie.cim.v0_82.vhd.MessageDocumentHeaderComplexType()
                                .withMessageDocumentHeaderMetaInformation(
                                        new energy.eddie.cim.v0_82.vhd.MessageDocumentHeaderMetaInformationComplexType()
                                                .withPermissionid("pid")
                                                .withConnectionid("cid")
                                                .withDataNeedid("dnid")
                                                .withDataType("validated-historical-data-market-document")
                                )
                );
        kafkaConnector.setEddieValidatedHistoricalDataMarketDocumentStream(Flux.just(data));
        var consumerProps = KafkaTestUtils.consumerProps(embeddedKafka, "testGroup", true);
        var consumer = new DefaultKafkaConsumerFactory<>(consumerProps,
                                                         new StringDeserializer(),
                                                         new StringDeserializer()).createConsumer();
        consumer.subscribe(Collections.singleton("ep.eddie.cim_0_82.validated-historical-data-md"));

        // When
        var records = KafkaTestUtils.getRecords(consumer);

        // Then
        assertThat(records).hasSize(1);
    }

    @Test
    void testAccountingPointDataMarketDocuments_areProducedToKafka() {
        // Given
        var data = new AccountingPointEnvelope()
                .withMessageDocumentHeader(
                        new energy.eddie.cim.v0_82.ap.MessageDocumentHeaderComplexType()
                                .withMessageDocumentHeaderMetaInformation(
                                        new energy.eddie.cim.v0_82.ap.MessageDocumentHeaderMetaInformationComplexType()
                                                .withPermissionid("pid")
                                                .withConnectionid("cid")
                                                .withDataNeedid("dnid")
                                                .withDataType("validated-historical-data-market-document")
                                )
                );
        kafkaConnector.setAccountingPointEnvelopeStream(Flux.just(data));
        var consumerProps = KafkaTestUtils.consumerProps(embeddedKafka, "testGroup", true);
        var consumer = new DefaultKafkaConsumerFactory<>(consumerProps,
                                                         new StringDeserializer(),
                                                         new StringDeserializer()).createConsumer();
        consumer.subscribe(Collections.singleton("ep.eddie.cim_0_82.accounting-point-md"));

        // When
        var records = KafkaTestUtils.getRecords(consumer);

        // Then
        assertThat(records).hasSize(1);
    }

    @Test
    void testRawData_areProducedToKafka() {
        // Given
        var data = new RawDataMessage(
                "pid",
                "cid",
                "dnid",
                dataSourceInformation,
                ZonedDateTime.now(ZoneOffset.UTC),
                "blblblb"
        );
        kafkaConnector.setRawDataStream(Flux.just(data));
        var consumerProps = KafkaTestUtils.consumerProps(embeddedKafka, "testGroup", true);
        var consumer = new DefaultKafkaConsumerFactory<>(consumerProps,
                                                         new StringDeserializer(),
                                                         new StringDeserializer()).createConsumer();
        consumer.subscribe(Collections.singleton("ep.eddie.agnostic.raw-data-message"));

        // When
        var records = KafkaTestUtils.getRecords(consumer);

        // Then
        assertThat(records).hasSize(1);
    }

    @Test
    void testCIM_v1_04_ValidatedHistoricalDataMarketDocuments_areProducedToKafka() {
        // Given
        var data = new VHDEnvelope()
                .withMessageDocumentHeaderMetaInformationPermissionId("pid")
                .withMessageDocumentHeaderMetaInformationConnectionId("cid")
                .withMessageDocumentHeaderMetaInformationDataNeedId("dnid")
                .withMessageDocumentHeaderMetaInformationDocumentType("validated-historical-data-market-document");
        kafkaConnector.setValidatedHistoricalDataMarketDocumentStream(Flux.just(data));
        var consumerProps = KafkaTestUtils.consumerProps(embeddedKafka, "testGroup", true);
        var consumer = new DefaultKafkaConsumerFactory<>(consumerProps,
                                                         new StringDeserializer(),
                                                         new StringDeserializer()).createConsumer();
        consumer.subscribe(Collections.singleton("ep.eddie.cim_1_04.validated-historical-data-md"));

        // When
        var records = KafkaTestUtils.getRecords(consumer);

        // Then
        assertThat(records).hasSize(1);
    }

    @Test
    void testNearRealTimeDataMarketDocuments_areProducedToKafka() {
        // Given
        var data = new RTDEnvelope()
                .withMessageDocumentHeaderMetaInformationPermissionId("pid")
                .withMessageDocumentHeaderMetaInformationConnectionId("cid")
                .withMessageDocumentHeaderMetaInformationDataNeedId("dnid")
                .withMessageDocumentHeaderMetaInformationDocumentType("near-real-time-data-market-document");
        kafkaConnector.setNearRealTimeDataMarketDocumentStream(Flux.just(data));
        var consumerProps = KafkaTestUtils.consumerProps(embeddedKafka, "testGroup", true);
        var consumer = new DefaultKafkaConsumerFactory<>(consumerProps,
                                                         new StringDeserializer(),
                                                         new StringDeserializer()).createConsumer();
        consumer.subscribe(Collections.singleton("ep.eddie.cim_1_04.near-real-time-data-md"));

        // When
        var records = KafkaTestUtils.getRecords(consumer);

        // Then
        assertThat(records).hasSize(1);
    }
}