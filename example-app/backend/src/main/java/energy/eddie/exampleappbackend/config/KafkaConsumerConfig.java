package energy.eddie.exampleappbackend.config;

import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.exampleappbackend.kafka.util.CustomDeserializer;
import energy.eddie.exampleappbackend.kafka.util.MessageSerde;
import energy.eddie.exampleappbackend.kafka.util.SerdeFactory;
import energy.eddie.exampleappbackend.kafka.util.SerdeInitializationException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

@Configuration
public class KafkaConsumerConfig {
    @Bean
    public MessageSerde serde() throws SerdeInitializationException {
        return SerdeFactory.getInstance().create("xml");
    }

    @Bean
    public ConsumerFactory<String, PermissionEnvelope> permissionEnvelopeConsumerFactory(KafkaProperties kafkaProperties, MessageSerde serde) {
        return new DefaultKafkaConsumerFactory<>(
                kafkaProperties.buildConsumerProperties(),
                new StringDeserializer(),
                new CustomDeserializer<>(serde, PermissionEnvelope.class));
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, PermissionEnvelope>> permissionEnvelopeListenerContainerFactory(
            ConsumerFactory<String, PermissionEnvelope> consumerFactory
    ) {
        var listenerContainerFactory = new ConcurrentKafkaListenerContainerFactory<String, PermissionEnvelope>();
        listenerContainerFactory.setConsumerFactory(consumerFactory);
        return listenerContainerFactory;
    }

    @Bean
    public ConsumerFactory<String, ValidatedHistoricalDataEnvelope> validatedHistoricalDataEnvelopeConsumerFactory(KafkaProperties kafkaProperties, MessageSerde serde) {
        return new DefaultKafkaConsumerFactory<>(
                kafkaProperties.buildConsumerProperties(),
                new StringDeserializer(),
                new CustomDeserializer<>(serde, ValidatedHistoricalDataEnvelope.class));
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, ValidatedHistoricalDataEnvelope>> validatedHistoricalDataEnvelopeListenerContainerFactory(
            ConsumerFactory<String, ValidatedHistoricalDataEnvelope> consumerFactory
    ) {
        var listenerContainerFactory = new ConcurrentKafkaListenerContainerFactory<String, ValidatedHistoricalDataEnvelope>();
        listenerContainerFactory.setConsumerFactory(consumerFactory);
        return listenerContainerFactory;
    }
}
