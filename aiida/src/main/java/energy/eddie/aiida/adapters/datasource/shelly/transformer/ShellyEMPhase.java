package energy.eddie.aiida.adapters.datasource.shelly.transformer;

import java.util.stream.Stream;

public enum ShellyEMPhase {
    TOTAL("total"),
    NEUTRAL("n"),
    PHASE_L1("a"),
    PHASE_L2("b"),
    PHASE_L3("c"),
    UNKNOWN("");

    private final String phasePrefix;

    ShellyEMPhase(String phasePrefix) {
        this.phasePrefix = phasePrefix;
    }

    public static ShellyEMPhase fromKey(String key) {
        return Stream.of(values())
                .filter(phase -> key.startsWith(phase.phasePrefix))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
