package energy.eddie.aiida.web.webhook.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ConnectionAcknowledgement {
    SUCCESS("success"),
    FAILURE("failure");

    private final String value;

    ConnectionAcknowledgement(String value) {this.value = value;}

    @JsonCreator
    public static ConnectionAcknowledgement fromValue(String value) {
        for (ConnectionAcknowledgement connAck : ConnectionAcknowledgement.values()) {
            if (connAck.toString().equals(value)) {
                return connAck;
            }
        }
        throw new IllegalArgumentException("Invalid connAck value: " + value);
    }

    @Override
    public String toString() {
        return this.value;
    }
}
