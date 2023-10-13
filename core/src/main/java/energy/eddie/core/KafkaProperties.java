package energy.eddie.core;

import org.eclipse.microprofile.config.Config;

import java.util.Properties;

public class KafkaProperties {

    private KafkaProperties() {
    }

    public static Properties fromConfig(Config config) {
        Properties kafkaProperties = new Properties();

        for (String name : config.getPropertyNames()) {
            if (name.startsWith("kafka.")) {
                String value = config.getValue(name, String.class);
                String propertyNameWithoutPrefix = name.substring("kafka.".length());
                kafkaProperties.setProperty(propertyNameWithoutPrefix, value);
            }
        }

        return kafkaProperties;
    }
}
