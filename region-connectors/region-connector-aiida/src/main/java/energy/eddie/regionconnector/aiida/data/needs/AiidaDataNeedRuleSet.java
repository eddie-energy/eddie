// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.data.needs;

import energy.eddie.dataneeds.rules.DataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRuleSet;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiidaDataNeedRuleSet implements DataNeedRuleSet {
    @Override
    public List<DataNeedRule> dataNeedRules() {
        return List.of(
                new DataNeedRule.InboundAiidaDataNeedRule(),
                new DataNeedRule.OutboundAiidaDataNeedRule(),
                new DataNeedRule.AllowMultipleDataNeedsRule()
        );
    }
}
