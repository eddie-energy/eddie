// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.data.needs;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimulationDataNeedRuleSetTest {

    @Test
    void givenSimulationDataNeedRuleSet_whenDataNeedRules_thenReturnOneForEachEnergyType() {
        // Given
        var ruleSet = new SimulationDataNeedRuleSet();

        // When
        var res = ruleSet.dataNeedRules();

        // Then
        assertThat(res).hasSize(EnergyType.values().length);
    }
}