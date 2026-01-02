package energy.eddie.spring.regionconnector.extensions.dataneeds;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.core.services.DataNeedRuleSetRouter;
import energy.eddie.dataneeds.supported.DataNeedRuleSet;
import energy.eddie.regionconnector.shared.services.data.needs.DefaultDataNeedRuleSet;
import org.springframework.beans.factory.ObjectProvider;

import static java.util.Objects.requireNonNull;

/**
 * The {@code DataNeedRuleSetRegistrar} will be added to each region connector's own context and will
 * register the {@link DataNeedRuleSet} of each region connector to the common
 * {@link DataNeedRuleSetRouter}.
 */
@RegionConnectorExtension
public class DataNeedRuleSetRegistrar {
    public DataNeedRuleSetRegistrar(
            ObjectProvider<DataNeedRuleSet> dataNeedRuleSet,
            RegionConnector regionConnector,
            DataNeedRuleSetRouter router
    ) {
        requireNonNull(regionConnector);
        requireNonNull(router);
        var rules = dataNeedRuleSet.getIfAvailable(() -> new DefaultDataNeedRuleSet(regionConnector.getMetadata()));
        router.register(regionConnector.getMetadata().id(), rules);
    }
}
