// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.rules.DataNeedRule.AccountingPointDataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRule.EnergyCommunityDataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRule.ValidatedHistoricalDataDataNeedRule;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EdaDataNeedRuleSetTest {
    @Test
    void testDataNeedRuleSet() {
        // Given
        var ruleSet = new EdaDataNeedRuleSet(new AtConfiguration("ep", "ec"));

        // When
        var res = ruleSet.dataNeedRules();

        // Then
        assertThat(res)
                .containsExactlyInAnyOrder(
                        new AccountingPointDataNeedRule(),
                        new ValidatedHistoricalDataDataNeedRule(EnergyType.ELECTRICITY,
                                                                List.of(Granularity.PT15M, Granularity.P1D)),
                        new EnergyCommunityDataNeedRule()
                );
    }

    @Test
    void testDataNeedRuleSet_withoutEnergyCommunityId_doesNotContainEnergyCommunityDataNeedRule() {
        // Given
        var ruleSet = new EdaDataNeedRuleSet(new AtConfiguration("ep", null));

        // When
        var res = ruleSet.dataNeedRules();

        // Then
        assertThat(res)
                .containsExactlyInAnyOrder(
                        new AccountingPointDataNeedRule(),
                        new ValidatedHistoricalDataDataNeedRule(EnergyType.ELECTRICITY,
                                                                List.of(Granularity.PT15M, Granularity.P1D))
                );
    }
}