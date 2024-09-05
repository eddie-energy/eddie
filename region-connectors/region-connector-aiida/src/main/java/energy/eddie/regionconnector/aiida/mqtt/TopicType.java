package energy.eddie.regionconnector.aiida.mqtt;

public enum TopicType {
    DATA("data"),
    STATUS("status"),
    TERMINATION("termination");

    private final String topicName;

    TopicType(String topicName) {
        this.topicName = topicName;
    }

    public String topicName() {
        return topicName;
    }
}