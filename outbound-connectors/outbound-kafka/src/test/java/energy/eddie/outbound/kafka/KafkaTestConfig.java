// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.kafka;

import energy.eddie.cim.serde.MessageSerde;
import energy.eddie.cim.serde.SerdeFactory;
import energy.eddie.cim.serde.SerdeInitializationException;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_91_08.RTREnvelope;
import energy.eddie.outbound.shared.TopicConfiguration;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.util.HashMap;

@TestConfiguration
@EmbeddedKafka(partitions = 1, topics = {"status-messages", "permission-market-documents", "validated-historical-data", "raw-data-in-proprietary-format", "accounting-point-market-documents", "terminations"})
public class KafkaTestConfig {
    @Bean
    public MessageSerde serde() throws SerdeInitializationException {
        return SerdeFactory.getInstance().create("json");
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") EmbeddedKafkaBroker embeddedKafka,
            MessageSerde serde
    ) {
        var props = new HashMap<String, Object>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props,
                                                                     new StringSerializer(),
                                                                     new CustomSerializer(serde)));
    }

    @Bean
    public KafkaTemplate<String, String> stringKafkaTemplate(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") EmbeddedKafkaBroker embeddedKafka) {
        var props = new HashMap<String, Object>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props,
                                                                     new StringSerializer(),
                                                                     new StringSerializer()));
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, PermissionEnvelope>> permissionEnvelopeListenerContainerFactory(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") EmbeddedKafkaBroker embeddedKafka,
            MessageSerde serde
    ) {
        var props = new HashMap<String, Object>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        var consumerFactory = new DefaultKafkaConsumerFactory<>(props,
                                                                new StringDeserializer(),
                                                                new CustomDeserializer<>(serde,
                                                                                         PermissionEnvelope.class));
        var listenerContainerFactory = new ConcurrentKafkaListenerContainerFactory<String, PermissionEnvelope>();
        listenerContainerFactory.setConsumerFactory(consumerFactory);
        return listenerContainerFactory;
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, RTREnvelope>> rtrEnvelopeListenerContainerFactory(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") EmbeddedKafkaBroker embeddedKafka,
            MessageSerde serde
    ) {
        var props = new HashMap<String, Object>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        var consumerFactory = new DefaultKafkaConsumerFactory<>(props,
                                                                new StringDeserializer(),
                                                                new CustomDeserializer<>(serde, RTREnvelope.class));
        var listenerContainerFactory = new ConcurrentKafkaListenerContainerFactory<String, RTREnvelope>();
        listenerContainerFactory.setConsumerFactory(consumerFactory);
        return listenerContainerFactory;
    }

    @Bean
    public TopicConfiguration topicConfiguration() {
        return new TopicConfiguration("eddie");
    }
}
