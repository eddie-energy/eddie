package energy.eddie.dataneeds.validation.asset;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum AiidaAsset {
    CONNECTION_AGREEMENT_POINT("Connection Agreement Point"),
    CONTROLLABLE_UNIT("Controllable Unit"),
    DEDICATED_MEASUREMENT_DEVICE("Dedicated Measurement Device"),
    SUBMETER("Submeter");

    private final String asset;

    AiidaAsset(String asset) {
        this.asset = asset;
    }

    @JsonCreator
    public static AiidaAsset forValue(String value) {
        return Arrays.stream(AiidaAsset.values())
                     .filter(op -> op.getValue().equals(value))
                     .findFirst()
                     .orElseThrow();
    }

    @Override
    public String toString() {
        return asset;
    }

    @JsonValue
    public String getValue() {
        return asset;
    }
}
