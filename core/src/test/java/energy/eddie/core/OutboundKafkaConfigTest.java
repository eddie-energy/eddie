package energy.eddie.core;

import energy.eddie.core.services.*;
import energy.eddie.outbound.kafka.KafkaConnector;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

/**
 * Tests that the {@link KafkaConnector} is only created if <i>kafka.enabled=true</i> and verifies that then the
 * according outbound connectors are created.
 */
class OutboundKafkaConfigTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(OutboundKafkaConfigTestTestConfig.class)
            .withUserConfiguration(OutboundKafkaConfig.class);

    @Test
    void givenEnabledIsFalse_kafkaBeanIsNotCreated() {
        contextRunner
                .withPropertyValues("kafka.enabled=false")
                .withPropertyValues("kafka.bootstrap.servers=localhost:9093")
                .run(context -> assertThat(context).doesNotHaveBean(KafkaConnector.class));
    }

    @Test
    void givenEnabledIsTrue_configurationMapIsFilled() {
        contextRunner
                .withPropertyValues("kafka.enabled=true")
                .withPropertyValues("kafka.bootstrap.servers=localhost:9093")
                .withPropertyValues("kafka.acks=all")
                .run(context -> {
                    assertThat(context).hasBean("kafkaPropertiesMap");

                    @SuppressWarnings("unchecked")  // needed because of type erasure...
                    Map<String, String> kafkaPropertiesMap = context.getBean("kafkaPropertiesMap", Map.class);

                    assertThat(kafkaPropertiesMap).containsEntry("bootstrap.servers", "localhost:9093");
                    assertThat(kafkaPropertiesMap).containsEntry("acks", "all");
                    assertThat(kafkaPropertiesMap).containsEntry("enabled", "true");
                });
    }

    @Test
    void givenEnabledIsTrue_kafkaPropertiesContainsAllButEnabled() {
        contextRunner
                .withPropertyValues("kafka.enabled=true")
                .withPropertyValues("kafka.bootstrap.servers=localhost:9093")
                .withPropertyValues("kafka.acks=all")
                .run(context -> {
                    assertThat(context).hasBean("kafkaProperties");
                    Properties kafkaProperties = context.getBean("kafkaProperties", Properties.class);

                    assertThat(kafkaProperties).containsEntry("bootstrap.servers", "localhost:9093");
                    assertThat(kafkaProperties).containsEntry("acks", "all");
                });
    }

    @Test
    void givenEnabledIsTrue_kafkaBeanIsCreated() {
        contextRunner
                .withPropertyValues("kafka.enabled=true")
                .withPropertyValues("kafka.bootstrap.servers=localhost:9093")
                .withPropertyValues("kafka.acks=all")
                .run(context -> assertThat(context).hasBean("kafkaConnector"));
    }

    @Test
    void givenEnabledIsTrue_differentOutboundConnectorsAreCreated() {
        contextRunner
                .withPropertyValues("kafka.enabled=true")
                .withPropertyValues("kafka.bootstrap.servers=localhost:9093")
                .withPropertyValues("kafka.acks=all")
                .run(context -> assertAll(
                        () -> assertThat(context).hasBean("kafkaConnector"),
                        () -> assertThat(context).hasBean("mvp1ConsumptionRecordOutboundConnector"),
                        () -> assertThat(context).hasBean("mvp1ConnectionStatusMessageOutboundConnector"),
                        () -> assertThat(context).hasBean("eddieValidatedHistoricalDataMarketDocumentOutboundConnector"),
                        () -> assertThat(context).hasBean("consentMarketDocumentService")
                ));
    }

    protected static class OutboundKafkaConfigTestTestConfig {
        @Bean
        public PermissionService permissionService() {
            PermissionService mock = Mockito.mock(PermissionService.class);
            when(mock.getConnectionStatusMessageStream()).thenReturn(Flux.empty());
            return mock;
        }

        @Bean
        public ConsumptionRecordService consumptionRecordService() {
            ConsumptionRecordService mock = Mockito.mock(ConsumptionRecordService.class);
            when(mock.getConsumptionRecordStream()).thenReturn(Flux.empty());
            return mock;
        }

        @Bean
        public EddieValidatedHistoricalDataMarketDocumentService eddieValidatedHistoricalDataMarketDocumentService() {
            EddieValidatedHistoricalDataMarketDocumentService mock = Mockito.mock(
                    EddieValidatedHistoricalDataMarketDocumentService.class);
            when(mock.getEddieValidatedHistoricalDataMarketDocumentStream()).thenReturn(Flux.empty());
            return mock;
        }

        @Bean
        public ConsentMarketDocumentService consentMarketDocumentService() {
            ConsentMarketDocumentService mock = Mockito.mock(ConsentMarketDocumentService.class);
            when(mock.getConsentMarketDocumentStream()).thenReturn(Flux.empty());
            return mock;
        }

        @Bean
        public EddieAccountingPointMarketDocumentService eddieAccountingPointMarketDocumentService() {
            EddieAccountingPointMarketDocumentService mock = Mockito.mock(EddieAccountingPointMarketDocumentService.class);
            when(mock.getEddieAccountingPointMarketDocumentStream()).thenReturn(Flux.empty());
            return mock;
        }
    }
}
