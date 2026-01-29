// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions.dataneeds;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.core.services.DataNeedRuleSetRouter;
import energy.eddie.dataneeds.rules.DataNeedRuleSet;
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
        dataNeedRuleSet.ifAvailable(rs -> router.register(regionConnector.getMetadata().id(), rs));
    }
}
