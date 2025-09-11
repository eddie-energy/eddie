package energy.eddie.aiida.adapters.datasource.shelly.transformer;

import java.util.stream.Stream;

public enum ShellyPhase {
    TOTAL("total"),
    NEUTRAL("n"),
    PHASE_L1("a"),
    PHASE_L2("b"),
    PHASE_L3("c"),
    UNKNOWN("unknown");

    private final String phasePrefix;

    ShellyPhase(String phasePrefix) {
        this.phasePrefix = phasePrefix;
    }

    public static ShellyPhase fromKey(String key) {
        return Stream.of(values())
                .filter(phase -> key.startsWith(phase.phasePrefix))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
