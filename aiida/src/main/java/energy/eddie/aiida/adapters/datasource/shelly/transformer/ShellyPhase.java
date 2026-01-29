// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.shelly.transformer;

import java.util.stream.Stream;

public enum ShellyPhase {
    TOTAL("total_"),
    NEUTRAL("n_"),
    PHASE_L1("a_"),
    PHASE_L2("b_"),
    PHASE_L3("c_"),
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
