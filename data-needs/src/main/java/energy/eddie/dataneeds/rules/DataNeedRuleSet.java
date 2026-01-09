package energy.eddie.dataneeds.rules;

import com.fasterxml.jackson.annotation.JsonValue;
import energy.eddie.dataneeds.needs.DataNeed;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This interface specifies all data need rules that are supported by a certain region connector.
 */
public interface DataNeedRuleSet {

    /**
     * Returns all the data need rules
     * @return all data need rules
     */
    @JsonValue
    @SuppressWarnings("java:S1452")
    // It is not possible to completely remove the wildcard type here, since this method might return a non-homogenous list.
    List<DataNeedRule<? extends DataNeed>> dataNeedRules();

    default Set<String> supportedDataNeeds() {
        var dataNeeds = new HashSet<String>();
        for (var rule : dataNeedRules()) {
            dataNeeds.add(rule.getType());
        }
        return dataNeeds;
    }
}
