package energy.eddie.spring.regionconnector.extensions.dataneeds;

import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.InboundAiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.OutboundAiidaDataNeed;
import energy.eddie.dataneeds.supported.DataNeedRule;
import energy.eddie.dataneeds.supported.DataNeedRule.AccountingPointDataNeedRule;
import energy.eddie.dataneeds.supported.DataNeedRule.AiidaDataNeedRule;
import energy.eddie.dataneeds.supported.DataNeedRule.ValidatedHistoricalDataDataNeedRule;
import energy.eddie.dataneeds.supported.DataNeedRuleSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DefaultDataNeedRuleSet implements DataNeedRuleSet {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDataNeedRuleSet.class);
    private final RegionConnectorMetadata metadata;

    public DefaultDataNeedRuleSet(RegionConnectorMetadata metadata) {
        LOGGER.atTrace()
              .addArgument(metadata::id)
              .log("Registering for region connector {}");
        this.metadata = metadata;
    }

    @Override
    public List<DataNeedRule<? extends DataNeed>> dataNeedRules() {
        var sdn = new ArrayList<DataNeedRule<? extends DataNeed>>();
        for (var supportedDataNeed : metadata.supportedDataNeeds()) {
            if (supportedDataNeed.isAssignableFrom(AccountingPointDataNeed.class)) {
                sdn.add(new AccountingPointDataNeedRule());
            } else if (supportedDataNeed.isAssignableFrom(ValidatedHistoricalDataDataNeed.class)) {
                for (var supportedEnergyType : metadata.supportedEnergyTypes()) {
                    sdn.add(new ValidatedHistoricalDataDataNeedRule(
                            supportedEnergyType,
                            metadata.granularitiesFor(supportedEnergyType),
                            metadata.earliestStart(),
                            metadata.latestEnd()
                    ));
                }
            } else if (supportedDataNeed.isAssignableFrom(InboundAiidaDataNeed.class)) {
                sdn.add(new AiidaDataNeedRule<>(InboundAiidaDataNeed.class));
            } else if (supportedDataNeed.isAssignableFrom(OutboundAiidaDataNeed.class)) {
                sdn.add(new AiidaDataNeedRule<>(OutboundAiidaDataNeed.class));
            } else {
                LOGGER.warn("Got unknown supported data need: {}", supportedDataNeed);
            }
        }
        return sdn;
    }
}
