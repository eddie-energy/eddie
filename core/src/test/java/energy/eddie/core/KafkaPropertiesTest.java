package energy.eddie.core;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaPropertiesTest {

    @Test
    void fromConfig_accessPropertyWithoutPrefix() {
        Config config = mock(Config.class);
        String expected = "expected";
        when(config.getPropertyNames()).thenReturn(Set.of("kafka.bootstrap.servers"));
        when(config.getValue("kafka.bootstrap.servers", String.class)).thenReturn(expected);

        var kafkaProperties = KafkaProperties.fromConfig(config);

        assertEquals(expected, kafkaProperties.getProperty("bootstrap.servers"));
    }


    @Test
    void fromConfig_accessPropertiesWithoutPrefix() {
        Config config = mock(Config.class);
        String expected = "expected";

        when(config.getPropertyNames()).thenReturn(Set.of("kafka.bootstrap.servers", "kafka.value.serializer", "kafka.key.serializer"));
        when(config.getValue("kafka.bootstrap.servers", String.class)).thenReturn(expected);
        when(config.getValue("kafka.value.serializer", String.class)).thenReturn(expected);
        when(config.getValue("kafka.key.serializer", String.class)).thenReturn(expected);

        var kafkaProperties = KafkaProperties.fromConfig(config);

        assertEquals(expected, kafkaProperties.getProperty("bootstrap.servers"));
        assertEquals(expected, kafkaProperties.getProperty("value.serializer"));
        assertEquals(expected, kafkaProperties.getProperty("key.serializer"));
    }

    @Test
    void fromConfig_ignoresPropertiesNotStartingWithKafka() {
        Config config = mock(Config.class);
        when(config.getPropertyNames()).thenReturn(Set.of("test", "value.test"));

        var kafkaProperties = KafkaProperties.fromConfig(config);

        assertTrue(kafkaProperties.isEmpty());
    }
}