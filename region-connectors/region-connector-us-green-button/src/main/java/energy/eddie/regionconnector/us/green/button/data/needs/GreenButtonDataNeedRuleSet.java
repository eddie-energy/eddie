package energy.eddie.regionconnector.us.green.button.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.rules.DataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRule.AccountingPointDataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRule.ValidatedHistoricalDataDataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRuleSet;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GreenButtonDataNeedRuleSet implements DataNeedRuleSet {
    public static final List<Granularity> SUPPORTED_GRANULARITIES = List.of(Granularity.PT15M, Granularity.P1D);

    @Override
    public List<DataNeedRule> dataNeedRules() {
        return List.of(
                new AccountingPointDataNeedRule(),
                new ValidatedHistoricalDataDataNeedRule(EnergyType.ELECTRICITY, SUPPORTED_GRANULARITIES),
                new ValidatedHistoricalDataDataNeedRule(EnergyType.NATURAL_GAS, SUPPORTED_GRANULARITIES),
                new ValidatedHistoricalDataDataNeedRule(EnergyType.HYDROGEN, SUPPORTED_GRANULARITIES),
                new ValidatedHistoricalDataDataNeedRule(EnergyType.HEAT, SUPPORTED_GRANULARITIES)
        );
    }
}
