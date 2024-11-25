package energy.eddie.outbound.kafka;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.agnostic.DataType;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.pmd.MessageDocumentHeaderComplexType;
import energy.eddie.cim.v0_82.pmd.MessageDocumentHeaderMetaInformationComplexType;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
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
        var consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafka);
        var consumer = new DefaultKafkaConsumerFactory<String, Object>(consumerProps).createConsumer();
        consumer.subscribe(Collections.singleton("status-messages"));

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
                                                .withDataType(DataType.HISTORICAL_VALIDATED_CONSUMPTION_DATA.name())
                                )
                );
        kafkaConnector.setPermissionMarketDocumentStream(Flux.just(data));
        var consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafka);
        var consumer = new DefaultKafkaConsumerFactory<>(consumerProps,
                                                         new StringDeserializer(),
                                                         new StringDeserializer()).createConsumer();
        consumer.subscribe(Collections.singleton("permission-market-documents"));

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
                                                .withDataType(DataType.HISTORICAL_VALIDATED_CONSUMPTION_DATA.name())
                                )
                );
        kafkaConnector.setEddieValidatedHistoricalDataMarketDocumentStream(Flux.just(data));
        var consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafka);
        var consumer = new DefaultKafkaConsumerFactory<>(consumerProps,
                                                         new StringDeserializer(),
                                                         new StringDeserializer()).createConsumer();
        consumer.subscribe(Collections.singleton("validated-historical-data"));

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
                                                .withDataType(DataType.HISTORICAL_VALIDATED_CONSUMPTION_DATA.name())
                                )
                );
        kafkaConnector.setAccountingPointEnvelopeStream(Flux.just(data));
        var consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafka);
        var consumer = new DefaultKafkaConsumerFactory<>(consumerProps,
                                                         new StringDeserializer(),
                                                         new StringDeserializer()).createConsumer();
        consumer.subscribe(Collections.singleton("accounting-point-market-documents"));

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
        var consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafka);
        var consumer = new DefaultKafkaConsumerFactory<>(consumerProps,
                                                         new StringDeserializer(),
                                                         new StringDeserializer()).createConsumer();
        consumer.subscribe(Collections.singleton("raw-data-in-proprietary-format"));

        // When
        var records = KafkaTestUtils.getRecords(consumer);

        // Then
        assertThat(records).hasSize(1);
    }
}