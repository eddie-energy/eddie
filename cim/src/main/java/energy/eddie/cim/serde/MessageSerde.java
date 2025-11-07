package energy.eddie.cim.serde;

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
}
