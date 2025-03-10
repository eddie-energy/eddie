package energy.eddie.dataneeds.needs.aiida;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum AiidaDirection {
    SUBSCRIBE("SUBSCRIBE"),
    PUBLISH("PUBLISH");

    private final String direction;

    AiidaDirection(String direction) {
        this.direction = direction;
    }

    @JsonCreator
    public static AiidaDirection forValue(String value) {
        return Arrays.stream(AiidaDirection.values())
                     .filter(op -> op.direction().equals(value))
                     .findFirst()
                     .orElseThrow();
    }

    @Override
    public String toString() {
        return direction;
    }

    @JsonValue
    public String direction() {
        return direction;
    }
}
