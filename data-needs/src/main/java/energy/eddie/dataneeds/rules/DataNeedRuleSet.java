// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.rules;

import com.fasterxml.jackson.annotation.JsonValue;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.rules.DataNeedRule.SpecificDataNeedRule;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This interface specifies all data need rules that are supported by a certain region connector.
 */
public interface DataNeedRuleSet {

    /**
     * Returns all the data need rules
     *
     * @return all data need rules
     */
    @JsonValue
    List<DataNeedRule> dataNeedRules();

    default boolean hasRule(DataNeedRule rule) {
        return dataNeedRules().contains(rule);
    }

    default boolean hasRuleFor(DataNeed dataNeed) {
        for (DataNeedRule rule : dataNeedRules()) {
            if (rule instanceof SpecificDataNeedRule<?> specificDataNeedRule
                && dataNeed.getClass().equals(specificDataNeedRule.getDataNeedClass())) {
                return true;
            }
        }
        return false;
    }

    default <T extends DataNeedRule> Set<T> dataNeedRules(Class<T> clazz) {
        var result = new HashSet<T>();
        for (var dataNeedRule : dataNeedRules()) {
            if (clazz.isAssignableFrom(dataNeedRule.getClass())) {
                result.add(clazz.cast(dataNeedRule));
            }
        }
        return result;
    }

    default Set<String> supportedDataNeeds() {
        var dataNeeds = new HashSet<String>();
        for (var rule : dataNeedRules()) {
            if (rule instanceof SpecificDataNeedRule<?> dataNeedRule) {
                dataNeeds.add(dataNeedRule.getType());
            }
        }
        return dataNeeds;
    }
}
