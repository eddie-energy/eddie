package energy.eddie.regionconnector.aiida.data.needs;

import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.aiida.InboundAiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.OutboundAiidaDataNeed;
import energy.eddie.dataneeds.rules.DataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRule.AiidaDataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRuleSet;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiidaDataNeedRuleSet implements DataNeedRuleSet {
    @Override
    public List<DataNeedRule<? extends DataNeed>> dataNeedRules() {
        return List.of(
                new AiidaDataNeedRule<>(InboundAiidaDataNeed.class),
                new AiidaDataNeedRule<>(OutboundAiidaDataNeed.class)
        );
    }
}
