package energy.eddie.regionconnector.aiida.exceptions;

public class AiidaMessageProcessorRegistryException extends Exception {
    public AiidaMessageProcessorRegistryException(String topic) {
        super("No AiidaMessageProcessor found for topic " + topic);
    }
}
