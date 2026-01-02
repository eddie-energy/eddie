package energy.eddie.regionconnector.simulation.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.supported.DataNeedRule;
import energy.eddie.dataneeds.supported.DataNeedRule.ValidatedHistoricalDataDataNeedRule;
import energy.eddie.dataneeds.supported.DataNeedRuleSet;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class SimulationDataNeedRuleSet implements DataNeedRuleSet {
    @Override
    public List<DataNeedRule<? extends DataNeed>> dataNeedRules() {
        var rules = new ArrayList<DataNeedRule<? extends DataNeed>>();
        for (var energyType : EnergyType.values()) {
            rules.add(new ValidatedHistoricalDataDataNeedRule(energyType, Arrays.asList(Granularity.values())));
        }
        return rules;
    }
}
