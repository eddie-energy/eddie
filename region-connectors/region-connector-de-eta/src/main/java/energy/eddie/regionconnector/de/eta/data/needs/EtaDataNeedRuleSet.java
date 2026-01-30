package energy.eddie.regionconnector.de.eta.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.rules.DataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRule.AccountingPointDataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRule.ValidatedHistoricalDataDataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRuleSet;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EtaDataNeedRuleSet implements DataNeedRuleSet {
    @Override
    public List<DataNeedRule<? extends DataNeed>> dataNeedRules() {
        return List.of(
                new AccountingPointDataNeedRule(),
                new ValidatedHistoricalDataDataNeedRule(EnergyType.ELECTRICITY, List.of(Granularity.PT15M, Granularity.PT1H, Granularity.P1D)),
                new ValidatedHistoricalDataDataNeedRule(EnergyType.NATURAL_GAS, List.of(Granularity.PT15M, Granularity.PT1H, Granularity.P1D))
        );
    }
}
