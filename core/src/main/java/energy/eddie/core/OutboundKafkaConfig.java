package energy.eddie.core;

import energy.eddie.api.agnostic.RawDataOutboundConnector;
import energy.eddie.api.v0.Mvp1ConnectionStatusMessageOutboundConnector;
import energy.eddie.api.v0_82.AccountingPointEnvelopeOutboundConnector;
import energy.eddie.api.v0_82.PermissionMarketDocumentOutboundConnector;
import energy.eddie.api.v0_82.ValidatedHistoricalDataEnvelopeOutboundConnector;
import energy.eddie.api.v0_82.outbound.TerminationConnector;
import energy.eddie.core.services.*;
import energy.eddie.outbound.kafka.KafkaConnector;
import energy.eddie.outbound.kafka.TerminationKafkaConnector;
import energy.eddie.regionconnector.shared.agnostic.OnRawDataMessagesEnabled;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Creates a {@link KafkaConnector} configured with all properties that start with <i>kafka</i> if the property
 * <i>kafka.enabled=true</i> is set.
 */
@Configuration
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
@EnableConfigurationProperties
public class OutboundKafkaConfig {
    private static final Set<String> CUSTOM_KEYS = Set.of("enabled", "termination.topic");

    @Bean(name = "kafkaPropertiesMap")
    @ConfigurationProperties(prefix = "kafka")
    public Map<String, String> kafkaPropertiesMap() {
        return new HashMap<>();
    }

    @Bean
    Properties kafkaProperties(@Qualifier("kafkaPropertiesMap") Map<String, String> kafkaPropertiesMap) {
        Properties kafkaProperties = new Properties();

        kafkaPropertiesMap.forEach((key, value) -> {
            // do not add the "enabled" property to KafkaClient config
            if (CUSTOM_KEYS.stream().noneMatch(key::equalsIgnoreCase))
                kafkaProperties.setProperty(key, value);
        });

        return kafkaProperties;
    }

    @Bean
    KafkaConnector kafkaConnector(Properties kafkaProperties) {
        return new KafkaConnector(kafkaProperties);
    }

    @Bean
    Mvp1ConnectionStatusMessageOutboundConnector mvp1ConnectionStatusMessageOutboundConnector(
            KafkaConnector kafkaConnector,
            PermissionService permissionService
    ) {
        ((Mvp1ConnectionStatusMessageOutboundConnector) kafkaConnector).setConnectionStatusMessageStream(
                permissionService.getConnectionStatusMessageStream());
        return kafkaConnector;
    }

    @Bean
    ValidatedHistoricalDataEnvelopeOutboundConnector validatedHistoricalDataEnvelopeOutboundConnector(
            KafkaConnector kafkaConnector,
            ValidatedHistoricalDataEnvelopeService cimService
    ) {
        kafkaConnector.setEddieValidatedHistoricalDataMarketDocumentStream(cimService.getEddieValidatedHistoricalDataMarketDocumentStream());
        return kafkaConnector;
    }

    @Bean
    PermissionMarketDocumentOutboundConnector permissionMarketDocumentOutboundConnector(
            KafkaConnector kafkaConnector,
            PermissionMarketDocumentService pmdService
    ) {
        kafkaConnector.setPermissionMarketDocumentStream(pmdService.getPermissionMarketDocumentStream());
        return kafkaConnector;
    }

    @Bean
    AccountingPointEnvelopeOutboundConnector accountingPointEnvelopeOutboundConnector(
            KafkaConnector kafkaConnector,
            AccountingPointEnvelopeService cimService
    ) {
        kafkaConnector.setAccountingPointEnvelopeStream(cimService.getAccountingPointEnvelopeStream());
        return kafkaConnector;
    }


    @Bean
    @OnRawDataMessagesEnabled
    RawDataOutboundConnector rawDataOutboundConnector(
            KafkaConnector kafkaConnector,
            RawDataService rawDataService
    ) {
        ((RawDataOutboundConnector) kafkaConnector).setRawDataStream(rawDataService.getRawDataStream());
        return kafkaConnector;
    }

    @Bean
    TerminationConnector terminationConnector(
            Properties kafkaProperties,
            @Value("${kafka.termination.topic:terminations}") String terminationTopic
    ) {
        return new TerminationKafkaConnector(kafkaProperties, terminationTopic);
    }
}
