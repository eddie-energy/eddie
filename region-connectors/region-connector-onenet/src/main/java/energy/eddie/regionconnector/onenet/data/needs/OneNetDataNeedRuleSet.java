// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.onenet.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.rules.DataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRule.ValidatedHistoricalDataDataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRuleSet;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OneNetDataNeedRuleSet implements DataNeedRuleSet {
    @Override
    public List<DataNeedRule> dataNeedRules() {
        // TODO: Update to match the data provided by onenet
        return List.of(
                new ValidatedHistoricalDataDataNeedRule(EnergyType.ELECTRICITY,
                                                        List.of(Granularity.PT15M, Granularity.PT1H, Granularity.P1D))
        );
    }
}
