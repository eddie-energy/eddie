package energy.eddie.aiida.adapters.datasource.shelly.transformer;

import java.util.stream.Stream;

public enum ShellyEMComponent {
    EM("em:0", ShellyEMPhase.TOTAL),
    EM1_0("em1:0", ShellyEMPhase.PHASE_L1),
    EM1_1("em1:1", ShellyEMPhase.PHASE_L2),
    EM1_2("em1:2", ShellyEMPhase.PHASE_L3),
    EM_DATA("emdata:0", ShellyEMPhase.TOTAL),
    EM1_DATA_0("em1data:0", ShellyEMPhase.PHASE_L1),
    EM1_DATA_1("em1data:1", ShellyEMPhase.PHASE_L2),
    EM1_DATA_2("em1data:2", ShellyEMPhase.PHASE_L3),
    UNKNOWN("unknown", ShellyEMPhase.UNKNOWN);

    private final String componentKey;
    private final ShellyEMPhase phase;

    ShellyEMComponent(String componentKey, ShellyEMPhase phase) {
        this.componentKey = componentKey;
        this.phase = phase;
    }

    public static ShellyEMComponent fromKey(String key) {
        return Stream.of(values())
                .filter(component -> key.startsWith(component.componentKey))
                .findFirst()
                .orElse(UNKNOWN);
    }

    public ShellyEMPhase phase() {
        return phase;
    }
}
