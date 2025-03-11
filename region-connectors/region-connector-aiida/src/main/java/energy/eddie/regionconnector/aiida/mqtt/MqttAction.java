package energy.eddie.regionconnector.aiida.mqtt;

public enum MqttAction {
    PUBLISH,
    SUBSCRIBE,
    ALL;

    public MqttAction getComplementaryAction() {
        return switch (this) {
            case PUBLISH -> SUBSCRIBE;
            case SUBSCRIBE -> PUBLISH;
            case ALL -> ALL;
        };
    }
}
