// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.rules.DataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRule.AccountingPointDataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRule.ValidatedHistoricalDataDataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRuleSet;
import energy.eddie.regionconnector.cds.client.CdsServerClient;
import energy.eddie.regionconnector.cds.client.CdsServerClientFactory;
import energy.eddie.regionconnector.cds.dtos.CdsServerMasterData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CdsDataNeedRuleSet implements DataNeedRuleSet {
    private static final Logger LOGGER = LoggerFactory.getLogger(CdsDataNeedRuleSet.class);
    private final CdsServerClientFactory factory;

    public CdsDataNeedRuleSet(CdsServerClientFactory factory) {this.factory = factory;}

    @Override
    public List<DataNeedRule<? extends DataNeed>> dataNeedRules() {
        var list = new ArrayList<DataNeedRule<? extends DataNeed>>();
        list.add(new AccountingPointDataNeedRule());
        Set<EnergyType> energyTypes;
        try {
            energyTypes = factory.getAll()
                                 .flatMap(CdsServerClient::masterData)
                                 .flatMapIterable(CdsServerMasterData::energyTypes)
                                 .collect(Collectors.toSet())
                                 .block();
        } catch (Exception e) {
            LOGGER.warn("Got error when requesting all energy types from registered CDS servers", e);
            return list;
        }
        if (energyTypes == null) {
            return list;
        }
        for (var energyType : energyTypes) {
            list.add(new ValidatedHistoricalDataDataNeedRule(energyType, Arrays.asList(Granularity.values())));
        }
        return list;
    }
}
