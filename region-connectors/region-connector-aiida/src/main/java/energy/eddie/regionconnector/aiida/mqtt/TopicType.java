package energy.eddie.regionconnector.aiida.mqtt;

public enum TopicType {
    OUTBOUND_DATA("data/outbound"),
    INBOUND_DATA("data/inbound"),
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