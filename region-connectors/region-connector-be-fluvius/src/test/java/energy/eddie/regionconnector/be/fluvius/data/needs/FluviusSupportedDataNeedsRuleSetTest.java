// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.rules.DataNeedRule.ValidatedHistoricalDataDataNeedRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FluviusSupportedDataNeedsRuleSetTest {
    @Test
    void givenFluviusDataNeedRuleSet_whenDataNeedRuleSet_thenReturnOnlyValidatedHistoricalDataDataNeedsRule() {
        // Given
        var specs = new FluviusDataNeedsRuleSet(false);

        // When
        var res = specs.dataNeedRules();

        // Then
        assertThat(res)
                .hasSize(2)
                .containsExactlyInAnyOrder(
                        new ValidatedHistoricalDataDataNeedRule(
                                EnergyType.ELECTRICITY,
                                List.of(Granularity.PT15M, Granularity.P1D)
                        ),
                        new ValidatedHistoricalDataDataNeedRule(
                                EnergyType.NATURAL_GAS,
                                List.of(Granularity.PT30M, Granularity.P1D)
                        )
                );
    }

    @Test
    void givenEnabledSandbox_whenDataNeedsRuleSet_thenReturnOnlyValidatedHistoricalDataDataNeedsRule() {
        // Given
        var specs = new FluviusDataNeedsRuleSet(true);

        // When
        var res = specs.dataNeedRules();

        // Then
        assertThat(res)
                .hasSize(2)
                .containsExactlyInAnyOrder(
                        new ValidatedHistoricalDataDataNeedRule(
                                EnergyType.ELECTRICITY,
                                List.of(Granularity.PT15M, Granularity.P1D)
                        ),
                        new ValidatedHistoricalDataDataNeedRule(
                                EnergyType.NATURAL_GAS,
                                List.of(Granularity.PT15M, Granularity.P1D)
                        )
                );
    }
}