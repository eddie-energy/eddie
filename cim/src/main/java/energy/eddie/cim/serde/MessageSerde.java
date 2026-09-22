// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.cim.serde;

import java.util.List;

/**
 * A serde is a SERializer-DEserializer, which supports one or multiple formats to serialize and deserialize objects to and from.
 */
public interface MessageSerde {
    /**
     * Serializes an object to a byte array.
     *
     * @param message the object to be serialized.
     * @return a serialized byte array
     * @throws SerializationException if the serialization failed
     */
    byte[] serialize(Object message) throws SerializationException;

    /**
     * Deserializes a message to a given class.
     *
     * @param message     the message that should be deserialized.
     * @param messageType the class of the destination type.
     * @param <T>         the destination type.
     * @return the deserialized object.
     * @throws DeserializationException if the deserialization fails.
     */
    <T> T deserialize(byte[] message, Class<T> messageType) throws DeserializationException;

    /**
     * Deserializes a message based on a {@link MessageType}.
     *
     * @param message     the message that should be deserialized.
     * @param messageType the type of the destination message.
     * @param <T>         the destination type.
     * @return the deserialized object.
     * @throws DeserializationException if the deserialization fails.
     */
    @SuppressWarnings("unchecked")
    default <T> T deserialize(byte[] message, MessageType messageType) throws DeserializationException {
        return (T) deserialize(message, (Class<Object>) messageType.messageClass());
    }

    /**
     * Deserializes a message to a list of elements based on the given elementType.
     *
     * @param message     the message that should be deserialized.
     * @param elementType the type of the elements of the destination list.
     * @param <T>         the element type.
     * @return the deserialized list.
     * @throws DeserializationException if the deserialization fails.
     */
    <T> List<T> deserializeList(byte[] message, Class<T> elementType) throws DeserializationException;

    /**
     * Deserializes a message to a list of elements based on a {@link MessageType}.
     *
     * @param message     the message that should be deserialized.
     * @param elementType the type of the elements of the destination list.
     * @param <T>         the element type.
     * @return the deserialized list.
     * @throws DeserializationException if the deserialization fails.
     */
    @SuppressWarnings("unchecked")
    default <T> List<T> deserializeList(byte[] message, MessageType elementType) throws DeserializationException {
        return deserializeList(message, (Class<T>) elementType.messageClass());
    }
}
