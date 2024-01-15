package energy.eddie.core;

import energy.eddie.api.agnostic.RawDataOutboundConnector;
import energy.eddie.api.v0.Mvp1ConnectionStatusMessageOutboundConnector;
import energy.eddie.api.v0.Mvp1ConsumptionRecordOutboundConnector;
import energy.eddie.api.v0_82.ConsentMarketDocumentOutboundConnector;
import energy.eddie.api.v0_82.EddieValidatedHistoricalDataMarketDocumentOutboundConnector;
import energy.eddie.core.services.ConsentMarketDocumentService;
import energy.eddie.core.services.ConsumptionRecordService;
import energy.eddie.core.services.EddieValidatedHistoricalDataMarketDocumentService;
import energy.eddie.core.services.PermissionService;
import energy.eddie.core.services.RawDataService;
import energy.eddie.outbound.kafka.KafkaConnector;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Creates a {@link KafkaConnector} configured with all properties that start with <i>kafka</i> if the property
 * <i>kafka.enabled=true</i> is set.
 */
@Configuration
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
@EnableConfigurationProperties
public class OutboundKafkaConfig {
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
            if (!key.equalsIgnoreCase("enabled"))
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
        ((Mvp1ConnectionStatusMessageOutboundConnector) kafkaConnector).setConnectionStatusMessageStream(permissionService.getConnectionStatusMessageStream());
        return kafkaConnector;
    }

    @Bean
    Mvp1ConsumptionRecordOutboundConnector mvp1ConsumptionRecordOutboundConnector(
            KafkaConnector kafkaConnector,
            ConsumptionRecordService consumptionRecordService
    ) {
        ((Mvp1ConsumptionRecordOutboundConnector) kafkaConnector).setConsumptionRecordStream(consumptionRecordService.getConsumptionRecordStream());
        return kafkaConnector;
    }

    @Bean
    EddieValidatedHistoricalDataMarketDocumentOutboundConnector eddieValidatedHistoricalDataMarketDocumentOutboundConnector(
            KafkaConnector kafkaConnector,
            EddieValidatedHistoricalDataMarketDocumentService cimService
    ) {
        ((EddieValidatedHistoricalDataMarketDocumentOutboundConnector) kafkaConnector).setEddieValidatedHistoricalDataMarketDocumentStream(cimService.getEddieValidatedHistoricalDataMarketDocumentStream());
        return kafkaConnector;
    }

    @Bean
    ConsentMarketDocumentOutboundConnector consentMarketDocumentOutboundConnector(
            KafkaConnector kafkaConnector,
            ConsentMarketDocumentService cmdService
    ) {
        kafkaConnector.setConsentMarketDocumentStream(cmdService.getConsentMarketDocumentStream());
        return kafkaConnector;
    }

    @Bean
    @ConditionalOnProperty(name = "eddie.raw.data.output.enabled", havingValue = "true")
    RawDataOutboundConnector rawDataOutboundConnector(
            KafkaConnector kafkaConnector,
            RawDataService rawDataService
    ) {
        ((RawDataOutboundConnector) kafkaConnector).setRawDataStream(rawDataService.getRawDataStream());
        return kafkaConnector;
    }
}
