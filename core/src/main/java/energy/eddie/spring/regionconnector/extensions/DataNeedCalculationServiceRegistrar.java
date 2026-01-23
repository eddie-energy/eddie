// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.core.services.DataNeedCalculationRouter;
import energy.eddie.dataneeds.needs.DataNeed;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The {@code DataNeedCalculationServiceRegistrar} will be added to each region connector's own context and will
 * register the {@link DataNeedCalculationService} of each region connector to the common
 * {@link DataNeedCalculationRouter}. Nothing happens, if a certain region connector does not have a
 * {@code DataNeedCalculationService} in its context.
 */
@RegionConnectorExtension
public class DataNeedCalculationServiceRegistrar {
    public DataNeedCalculationServiceRegistrar(
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
            Optional<DataNeedCalculationService<DataNeed>> dataNeedCalculationService,
            DataNeedCalculationRouter router
    ) {
        requireNonNull(dataNeedCalculationService);
        requireNonNull(router);
        dataNeedCalculationService.ifPresent(router::register);
    }
}
