package energy.eddie.outbound.shared.serde;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * A {@link MessageSerde} that serializes to JSON.
 * Respects jakarta XML annotation.
 */
public class JsonMessageSerde implements MessageSerde {
    private final ObjectMapper objectMapper;

    public JsonMessageSerde() {objectMapper = ObjectMapperCreator.create(Format.JSON);}

    @Override
    public byte[] serialize(Object message) throws SerializationException {
        try {
            return objectMapper.writeValueAsBytes(message);
        } catch (JsonProcessingException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] message, Class<T> messageType) throws DeserializationException {
        try {
            return objectMapper.readValue(message, messageType);
        } catch (IOException e) {
            throw new DeserializationException(e);
        }
    }
}
