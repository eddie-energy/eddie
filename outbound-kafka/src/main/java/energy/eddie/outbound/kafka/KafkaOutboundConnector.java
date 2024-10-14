package energy.eddie.outbound.kafka;

import energy.eddie.api.agnostic.OutboundConnector;
import energy.eddie.api.v0_82.outbound.TerminationConnector;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@OutboundConnector(name = "kafka")
@SpringBootApplication
public class KafkaOutboundConnector {
    private static final Set<String> CUSTOM_KEYS = Set.of("termination.topic");

    @Bean(name = "kafkaPropertiesMap")
    @ConfigurationProperties(prefix = "kafka")
    public Map<String, String> kafkaPropertiesMap() {
        return new HashMap<>();
    }

    @Bean
    Properties kafkaProperties(@Qualifier("kafkaPropertiesMap") Map<String, String> kafkaPropertiesMap) {
        var kafkaProperties = new Properties();
        kafkaPropertiesMap.forEach((key, value) -> {
            // do not add custom properties to KafkaClient config
            if (CUSTOM_KEYS.stream().noneMatch(key::equalsIgnoreCase)) {
                kafkaProperties.setProperty(key, value);
            }
        });
        return kafkaProperties;
    }

    @Bean
    KafkaConnector kafkaConnector(Properties kafkaProperties) {
        return new KafkaConnector(kafkaProperties);
    }


    @Bean
    TerminationConnector terminationConnector(
            Properties kafkaProperties,
            @Value("${kafka.termination.topic:terminations}") String terminationTopic
    ) {
        return new TerminationKafkaConnector(kafkaProperties, terminationTopic);
    }
}
