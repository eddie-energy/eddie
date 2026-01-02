package energy.eddie.regionconnector.es.datadis.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.supported.DataNeedRule;
import energy.eddie.dataneeds.supported.DataNeedRule.AccountingPointDataNeedRule;
import energy.eddie.dataneeds.supported.DataNeedRule.ValidatedHistoricalDataDataNeedRule;
import energy.eddie.dataneeds.supported.DataNeedRuleSet;
import org.springframework.stereotype.Component;

import java.util.List;

import static energy.eddie.api.agnostic.Granularity.PT15M;
import static energy.eddie.api.agnostic.Granularity.PT1H;

@Component
public class DatadisDataNeedRuleSet implements DataNeedRuleSet {
    public static final List<Granularity> SUPPORTED_GRANULARITIES = List.of(PT15M, PT1H);

    @Override
    public List<DataNeedRule<? extends DataNeed>> dataNeedRules() {
        return List.of(
                new AccountingPointDataNeedRule(),
                new ValidatedHistoricalDataDataNeedRule(EnergyType.ELECTRICITY, SUPPORTED_GRANULARITIES)
        );
    }
}
