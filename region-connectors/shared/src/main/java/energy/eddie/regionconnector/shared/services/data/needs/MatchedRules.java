package energy.eddie.regionconnector.shared.services.data.needs;

import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.supported.DataNeedRule;

import java.util.List;
import java.util.Optional;

sealed interface MatchedRules {

    record NoMatch() implements MatchedRules {
    }

    record Matched() implements MatchedRules {
    }

    record MatchedVHDRules(
            List<DataNeedRule.ValidatedHistoricalDataDataNeedRule> rules,
            ValidatedHistoricalDataDataNeed dataNeed
    ) implements MatchedRules {
        public Optional<DataNeedRule.ValidatedHistoricalDataDataNeedRule> forEnergyType() {
            for (var rule : rules) {
                if (rule.energyType().equals(dataNeed.energyType())) {
                    return Optional.of(rule);
                }
            }
            return Optional.empty();
        }
    }
}
