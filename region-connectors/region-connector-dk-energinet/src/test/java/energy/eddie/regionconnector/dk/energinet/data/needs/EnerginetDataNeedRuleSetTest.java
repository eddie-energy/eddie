// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.data.needs;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.rules.DataNeedRule.AccountingPointDataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRule.ValidatedHistoricalDataDataNeedRule;
import org.junit.jupiter.api.Test;

import static energy.eddie.regionconnector.dk.energinet.data.needs.EnerginetDataNeedRuleSet.SUPPORTED_GRANULARITIES;
import static org.assertj.core.api.Assertions.assertThat;

class EnerginetDataNeedRuleSetTest {

    @Test
    void testRuleSet() {
        // Given
        var ruleSet = new EnerginetDataNeedRuleSet();

        // When
        var res = ruleSet.dataNeedRules();

        // Then
        assertThat(res)
                .containsExactlyInAnyOrder(
                        new AccountingPointDataNeedRule(),
                        new ValidatedHistoricalDataDataNeedRule(EnergyType.ELECTRICITY, SUPPORTED_GRANULARITIES),
                        new ValidatedHistoricalDataDataNeedRule(EnergyType.NATURAL_GAS, SUPPORTED_GRANULARITIES)
                );
    }
}