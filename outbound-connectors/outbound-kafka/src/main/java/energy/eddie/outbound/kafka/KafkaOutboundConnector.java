package energy.eddie.outbound.kafka;

import energy.eddie.api.agnostic.outbound.OutboundConnector;
import energy.eddie.cim.serde.MessageSerde;
import energy.eddie.cim.serde.SerdeFactory;
import energy.eddie.cim.serde.SerdeInitializationException;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_91_08.RTREnvelope;
import energy.eddie.outbound.shared.TopicConfiguration;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jspecify.annotations.NonNull;
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
    public MessageSerde serde(@Value("${outbound-connector.kafka.format:json}") String format) throws SerdeInitializationException {
        return SerdeFactory.getInstance().create(format);
    }

    @Bean
    public ConsumerFactory<String, PermissionEnvelope> consumerFactory(
            @Qualifier("kafkaPropertiesMap") Map<String, String> kafkaProperties,
            MessageSerde serde
    ) {
        var config = kafkaProperties(kafkaProperties);
        return new DefaultKafkaConsumerFactory<>(config,
                                                 new StringDeserializer(),
                                                 new CustomDeserializer<>(serde, PermissionEnvelope.class));
    }

    @Bean
    public KafkaListenerContainerFactory<@NonNull ConcurrentMessageListenerContainer<String, PermissionEnvelope>> permissionEnvelopeListenerContainerFactory(
            ConsumerFactory<String, PermissionEnvelope> consumerFactory
    ) {
        var listenerContainerFactory = new ConcurrentKafkaListenerContainerFactory<String, PermissionEnvelope>();
        listenerContainerFactory.setConsumerFactory(consumerFactory);
        return listenerContainerFactory;
    }

    @Bean
    public ConsumerFactory<String, RTREnvelope> rtrEnvelopeConsumerFactory(
            @Qualifier("kafkaPropertiesMap") Map<String, String> kafkaProperties,
            MessageSerde serde
    ) {
        var config = kafkaProperties(kafkaProperties);
        return new DefaultKafkaConsumerFactory<>(config,
                                                 new StringDeserializer(),
                                                 new CustomDeserializer<>(serde, RTREnvelope.class));
    }

    @Bean
    public KafkaListenerContainerFactory<@NonNull ConcurrentMessageListenerContainer<String, RTREnvelope>> rtrEnvelopeListenerContainerFactory(
            ConsumerFactory<String, RTREnvelope> consumerFactory
    ) {
        var listenerContainerFactory = new ConcurrentKafkaListenerContainerFactory<String, RTREnvelope>();
        listenerContainerFactory.setConsumerFactory(consumerFactory);
        return listenerContainerFactory;
    }

    @Bean
    public TopicConfiguration topicConfiguration(@Value("${outbound-connector.kafka.eddie-id}") String eddieId) {
        return new TopicConfiguration(eddieId);
    }


    @Bean
    public NewTopic terminationTopic(TopicConfiguration config) {
        var topic = config.terminationMarketDocument();
        return TopicBuilder.name(topic).build();
    }

    @Bean
    public NewTopic retransmissionTopic(TopicConfiguration config) {
        var topic = config.redistributionTransactionRequestDocument();
        return TopicBuilder.name(topic).build();
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory(
            @Qualifier("kafkaPropertiesMap") Map<String, String> kafkaProperties,
            MessageSerde serde
    ) {
        var config = kafkaProperties(kafkaProperties);
        return new DefaultKafkaProducerFactory<>(config, new StringSerializer(), new CustomSerializer(serde));
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
