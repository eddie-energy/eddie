// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.services;

import energy.eddie.core.dtos.SupportedDataNeeds;
import energy.eddie.dataneeds.rules.DataNeedRuleSet;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class DataNeedRuleSetRouter {
    private final Map<String, DataNeedRuleSet> dataNeedRuleSets = new HashMap<>();

    public void register(String regionConnectorId, DataNeedRuleSet specs) {
        dataNeedRuleSets.put(regionConnectorId, specs);
    }

    public Map<String, DataNeedRuleSet> dataNeedRuleSets() {
        return Map.copyOf(dataNeedRuleSets);
    }

    public DataNeedRuleSet dataNeedRuleSets(String regionConnectorId) throws UnknownRegionConnectorException {
        var rules = dataNeedRuleSets.get(regionConnectorId);
        if (rules == null) {
            throw new UnknownRegionConnectorException(regionConnectorId);
        }
        return rules;
    }

    public Set<SupportedDataNeeds> supportedDataNeeds() {
        var set = new HashSet<SupportedDataNeeds>();
        for (var entry : dataNeedRuleSets.entrySet()) {
            set.add(new SupportedDataNeeds(entry.getKey(), entry.getValue().supportedDataNeeds()));
        }
        return set;
    }
}
