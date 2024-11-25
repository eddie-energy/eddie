package energy.eddie.outbound.shared.serde;

public class DefaultSerdeFactory implements SerdeFactory {
    /**
     * Creates a {@link MessageSerde} that supports JSON or XML.
     *
     * @param format Allowed values are {@code json} or {@code xml}.
     * @return {@link MessageSerde} for JSON or XML.
     * @throws SerdeInitializationException if the format is neither {@code json} nor {@code xml} or initialization of the {@link XmlMessageSerde} failed.
     */
    @Override
    public MessageSerde create(String format) throws SerdeInitializationException {
        return switch (format) {
            case "json" -> new JsonMessageSerde();
            case "xml" -> new XmlMessageSerde();
            default -> throw new SerdeInitializationException("Unknown format: " + format);
        };
    }
}
