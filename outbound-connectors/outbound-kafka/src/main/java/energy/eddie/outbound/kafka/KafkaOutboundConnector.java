package energy.eddie.outbound.kafka;

import energy.eddie.api.agnostic.OutboundConnector;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@OutboundConnector(name = "kafka")
@SpringBootApplication
@EnableKafka
public class KafkaOutboundConnector {
    private static final Set<String> CUSTOM_KEYS = Set.of("termination.topic");

    @Bean(name = "kafkaPropertiesMap")
    @ConfigurationProperties(prefix = "kafka")
    public Map<String, String> kafkaPropertiesMap() {
        return new HashMap<>();
    }

    @Bean
    public ConsumerFactory<String, PermissionEnvelope> consumerFactory(@Qualifier("kafkaPropertiesMap") Map<String, String> kafkaProperties) {
        var config = kafkaProperties(kafkaProperties);
        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), new CustomDeserializer());
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, PermissionEnvelope>> listenerContainerFactory(
            ConsumerFactory<String, PermissionEnvelope> consumerFactory
    ) {
        var listenerContainerFactory = new ConcurrentKafkaListenerContainerFactory<String, PermissionEnvelope>();
        listenerContainerFactory.setConsumerFactory(consumerFactory);
        return listenerContainerFactory;
    }


    @Bean
    public NewTopic terminationTopic(@Value("${kafka.termination.topic:terminations}") String terminationTopic) {
        return TopicBuilder.name(terminationTopic).build();
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory(@Qualifier("kafkaPropertiesMap") Map<String, String> kafkaProperties) {
        var config = kafkaProperties(kafkaProperties);
        return new DefaultKafkaProducerFactory<>(config, new StringSerializer(), new CustomSerializer());
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    private Map<String, Object> kafkaProperties(Map<String, String> kafkaPropertiesMap) {
        var kafkaProperties = new HashMap<String, Object>();
        kafkaPropertiesMap.forEach((key, value) -> {
            // do not add custom properties to KafkaClient config
            if (CUSTOM_KEYS.stream().noneMatch(key::equalsIgnoreCase)) {
                kafkaProperties.put(key, value);
            }
        });
        return kafkaProperties;
    }
}
