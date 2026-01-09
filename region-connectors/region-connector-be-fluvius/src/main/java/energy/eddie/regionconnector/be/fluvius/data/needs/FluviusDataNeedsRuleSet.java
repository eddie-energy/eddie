package energy.eddie.regionconnector.be.fluvius.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.rules.DataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRule.ValidatedHistoricalDataDataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRuleSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FluviusDataNeedsRuleSet implements DataNeedRuleSet {
    private final boolean sandboxEnabled;

    public FluviusDataNeedsRuleSet(
            @Value("${region-connector.be.fluvius.mock-mandates:false}") boolean sandboxEnabled
    ) {
        this.sandboxEnabled = sandboxEnabled;
    }

    @Override
    public List<DataNeedRule<? extends DataNeed>> dataNeedRules() {
        var granularitiesForGas = sandboxEnabled
                ? List.of(Granularity.PT15M, Granularity.P1D)
                : List.of(Granularity.PT30M, Granularity.P1D);
        return List.of(
                new ValidatedHistoricalDataDataNeedRule(
                        EnergyType.ELECTRICITY,
                        List.of(Granularity.PT15M, Granularity.P1D)
                ),
                new ValidatedHistoricalDataDataNeedRule(
                        EnergyType.NATURAL_GAS,
                        granularitiesForGas
                )
        );
    }
}
