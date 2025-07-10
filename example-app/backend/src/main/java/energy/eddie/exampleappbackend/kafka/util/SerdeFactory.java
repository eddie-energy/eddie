package energy.eddie.exampleappbackend.kafka.util;


/**
 * Factory to create {@link MessageSerde} instances.
 */
public interface SerdeFactory {
    /**
     * Creates a new {@link DefaultSerdeFactory}, that supports JSON and XML.
     *
     * @return a SerdeFactory that supports JSON and XML.
     */
    static SerdeFactory getInstance() {
        return new DefaultSerdeFactory();
    }

    /**
     * Creates an instance of a {@link MessageSerde} that supports the given format.
     *
     * @param format message format which should be serialized/deserialized
     * @return an instance of the {@link MessageSerde} for the given format.
     * @throws SerdeInitializationException if initialization of the {@link MessageSerde} instance has failed or the requested format is not supported.
     */
    MessageSerde create(String format) throws SerdeInitializationException;
}
