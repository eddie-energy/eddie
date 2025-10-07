package energy.eddie.aiida.adapters.datasource.shelly.transformer;

import java.util.stream.Stream;

public enum ShellyComponent {
    EM("em:0", ShellyPhase.TOTAL),
    EM1_0("em1:0", ShellyPhase.PHASE_L1),
    EM1_1("em1:1", ShellyPhase.PHASE_L2),
    EM1_2("em1:2", ShellyPhase.PHASE_L3),
    EM_DATA("emdata:0", ShellyPhase.TOTAL),
    EM1_DATA_0("em1data:0", ShellyPhase.PHASE_L1),
    EM1_DATA_1("em1data:1", ShellyPhase.PHASE_L2),
    EM1_DATA_2("em1data:2", ShellyPhase.PHASE_L3),
    SWITCH_0("switch:0", ShellyPhase.PHASE_L1),
    SWITCH_1("switch:1", ShellyPhase.PHASE_L1),
    SWITCH_2("switch:2", ShellyPhase.PHASE_L1),
    SWITCH_3("switch:3", ShellyPhase.PHASE_L1),
    UNKNOWN("unknown", ShellyPhase.UNKNOWN);

    private final String key;
    private final ShellyPhase phase;

    ShellyComponent(String key, ShellyPhase phase) {
        this.key = key;
        this.phase = phase;
    }

    public static ShellyComponent fromKey(String componentKey) {
        return Stream.of(values())
                .filter(component -> componentKey.equals(component.key))
                .findFirst()
                .orElse(UNKNOWN);
    }

    public String key() {
        return key;
    }

    public ShellyPhase phase() {
        return phase;
    }
}
