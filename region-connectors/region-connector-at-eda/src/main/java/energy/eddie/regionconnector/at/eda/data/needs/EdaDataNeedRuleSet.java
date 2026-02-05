// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.rules.DataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRule.AccountingPointDataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRule.EnergyCommunityDataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRule.ValidatedHistoricalDataDataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRuleSet;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class EdaDataNeedRuleSet implements DataNeedRuleSet {
    private static final List<Granularity> SUPPORTED_GRANULARITIES = List.of(Granularity.PT15M, Granularity.P1D);
    private static final Logger LOGGER = LoggerFactory.getLogger(EdaDataNeedRuleSet.class);
    private final AtConfiguration config;

    public EdaDataNeedRuleSet(AtConfiguration config) {this.config = config;}

    @Override
    public List<DataNeedRule> dataNeedRules() {
        List<DataNeedRule> dataNeedRules = new ArrayList<>();
        dataNeedRules.add(new AccountingPointDataNeedRule());
        dataNeedRules.add(new ValidatedHistoricalDataDataNeedRule(EnergyType.ELECTRICITY, SUPPORTED_GRANULARITIES));
        if (config.energyCommunityId() != null) {
            LOGGER.info(
                    "Energy Community ID present, enabling the energy community data need for the AT EDA Region Connector");
            dataNeedRules.add(new EnergyCommunityDataNeedRule());
        } else {
            LOGGER.info(
                    "Energy Community ID not present, disabling the energy community data need for the AT EDA Region Connector");
        }
        return dataNeedRules;
    }
}
