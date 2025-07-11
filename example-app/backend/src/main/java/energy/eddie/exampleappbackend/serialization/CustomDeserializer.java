package energy.eddie.exampleappbackend.serialization;

import jakarta.annotation.Nullable;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomDeserializer<T> implements Deserializer<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomDeserializer.class);
    private final MessageSerde serde;
    private final Class<T> clazz;

    public CustomDeserializer(MessageSerde serde, Class<T> clazz) {
        this.serde = serde;
        this.clazz = clazz;
    }

    @Override
    @Nullable
    public T deserialize(String topic, byte[] data) {
        try {
            return serde.deserialize(data, clazz);
        } catch (DeserializationException e) {
            LOGGER.info("Got invalid termination document", e);
            return null;
        }
    }
}
