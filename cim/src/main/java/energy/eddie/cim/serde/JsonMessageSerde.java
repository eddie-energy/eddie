// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.cim.serde;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * A {@link MessageSerde} that serializes to JSON.
 * Respects jakarta XML annotation.
 */
public class JsonMessageSerde implements MessageSerde {
    private final ObjectMapper objectMapper;

    public JsonMessageSerde() {objectMapper = ObjectMapperCreator.create(SerializationFormat.JSON);}

    @Override
    public byte[] serialize(Object message) throws SerializationException {
        try {
            return objectMapper.writeValueAsBytes(message);
        } catch (JacksonException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] message, Class<T> messageType) throws DeserializationException {
        try {
            return objectMapper.readValue(message, messageType);
        } catch (JacksonException e) {
            throw new DeserializationException(e);
        }
    }

    @Override
    public <T> List<T> deserializeList(byte[] message, Class<T> elementType) throws DeserializationException {
        try {
            return objectMapper.readValue(
                    message,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, elementType)
            );
        } catch (JacksonException e) {
            throw new DeserializationException(e);
        }
    }
}
