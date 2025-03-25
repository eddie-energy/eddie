package energy.eddie.aiida.models.datasource;

import java.util.Arrays;

public enum MqttAction {
    SUBSCRIBE(1),
    PUBLISH(2);

    private final Integer id;

    MqttAction(Integer id) {
        this.id = id;
    }

    public static MqttAction forId(Integer id) {
        return Arrays.stream(MqttAction.values())
                     .filter(op -> op.id.equals(id))
                     .findFirst()
                     .orElseThrow();
    }
}

