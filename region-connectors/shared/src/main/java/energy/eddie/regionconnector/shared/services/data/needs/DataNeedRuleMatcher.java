package energy.eddie.regionconnector.shared.services.data.needs;

import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.rules.DataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRuleSet;
import energy.eddie.regionconnector.shared.services.data.needs.MatchedRules.Matched;
import energy.eddie.regionconnector.shared.services.data.needs.MatchedRules.MatchedVHDRules;
import energy.eddie.regionconnector.shared.services.data.needs.MatchedRules.NoMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static energy.eddie.dataneeds.rules.DataNeedRule.ValidatedHistoricalDataDataNeedRule;

/**
 * This class checks a {@link DataNeedRuleSet} for rules that match the provided {@link DataNeed}.
 */
class DataNeedRuleMatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataNeedRuleMatcher.class);
    private final DataNeed dataNeed;
    private final DataNeedRuleSet ruleSet;

    public DataNeedRuleMatcher(DataNeed dataNeed, DataNeedRuleSet ruleSet) {
        this.dataNeed = dataNeed;
        this.ruleSet = ruleSet;
    }

    /**
     * Finds the rules that are applicable to the provided {@link DataNeed}.
     * For simple cases such as the {@link energy.eddie.dataneeds.needs.AccountingPointDataNeed} and {@link energy.eddie.dataneeds.needs.aiida.AiidaDataNeed} the returned result is only of the {@link Matched} type.
     * The {@link ValidatedHistoricalDataDataNeed} requires a more complex approach, because there could be multiple matching rules that are relevant for the user.
     * In that case the {@link MatchedVHDRules} object is returned.
     * If there are different types of rules matching the same {@link DataNeed} type, {@link NoMatch} will be returned, since the rules must be homogenous for each data need type.
     *
     * @return Whether any of the rules matched and in special cases, the matched rules.
     */
    public MatchedRules find() {
        var matchingRules = dataNeedRuleFor();
        if (matchingRules.isEmpty()) {
            return new NoMatch();
        }
        var expectedClass = matchingRules.getFirst().getClass();
        var isHomogeneous = matchingRules
                .stream()
                .allMatch(rule -> rule.getClass().equals(expectedClass));
        if (!isHomogeneous) {
            LOGGER.error("Invalid data need rule result, where for one data need {} different rules apply! {}",
                         dataNeed,
                         matchingRules);
            return new NoMatch();
        }
        if ((matchingRules.getFirst() instanceof ValidatedHistoricalDataDataNeedRule)) {
            var list = new ArrayList<ValidatedHistoricalDataDataNeedRule>();
            for (var matchingRule : matchingRules) {
                list.add((ValidatedHistoricalDataDataNeedRule) matchingRule);
            }
            return new MatchedVHDRules(list, (ValidatedHistoricalDataDataNeed) dataNeed);
        }
        return new Matched();
    }


    private List<DataNeedRule<?>> dataNeedRuleFor() {
        var list = new ArrayList<DataNeedRule<?>>();
        for (DataNeedRule<? extends DataNeed> rule : ruleSet.dataNeedRules()) {
            if (rule.getDataNeedClass().equals(dataNeed.getClass())) {
                list.add(rule);
            }
        }
        return list;
    }
}
