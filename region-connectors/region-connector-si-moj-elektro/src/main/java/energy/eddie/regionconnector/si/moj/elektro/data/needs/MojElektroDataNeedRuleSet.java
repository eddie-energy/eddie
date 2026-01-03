package energy.eddie.regionconnector.si.moj.elektro.data.needs;

import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.rules.DataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRuleSet;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MojElektroDataNeedRuleSet implements DataNeedRuleSet {
    @Override
    public List<DataNeedRule<? extends DataNeed>> dataNeedRules() {
        return List.of();
    }
}
