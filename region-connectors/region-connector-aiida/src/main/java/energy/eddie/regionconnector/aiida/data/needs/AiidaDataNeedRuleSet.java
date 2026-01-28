package energy.eddie.regionconnector.aiida.data.needs;

import energy.eddie.dataneeds.rules.DataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRuleSet;
import org.springframework.stereotype.Component;

import java.util.List;

import static energy.eddie.dataneeds.rules.DataNeedRule.AiidaDataNeedRule;
import static energy.eddie.dataneeds.rules.DataNeedRule.AiidaDataNeedRule.AiidaDataNeedTypes.INBOUND;
import static energy.eddie.dataneeds.rules.DataNeedRule.AiidaDataNeedRule.AiidaDataNeedTypes.OUTBOUND;

@Component
public class AiidaDataNeedRuleSet implements DataNeedRuleSet {
    @Override
    public List<DataNeedRule> dataNeedRules() {
        return List.of(
                new AiidaDataNeedRule(INBOUND),
                new AiidaDataNeedRule(OUTBOUND)
        );
    }
}
