package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.v0.Mvp1ConsumptionRecordProvider;
import energy.eddie.core.services.ConsumptionRecordService;
import energy.eddie.spring.RegionConnectorExtension;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The {@code ConsumptionRecordServiceRegistrar} will be added to each region connector's own context and will
 * register the {@link Mvp1ConsumptionRecordProvider} of each region connector to the common {@link ConsumptionRecordService}.
 * Nothing happens, if a certain region connector does not have a {@code Mvp1ConsumptionRecordProvider} in its context.
 */
@RegionConnectorExtension
public class ConsumptionRecordServiceRegistrar {
    public ConsumptionRecordServiceRegistrar(Optional<Mvp1ConsumptionRecordProvider> consumptionRecordProvider,
                                             ConsumptionRecordService consumptionRecordService) {
        requireNonNull(consumptionRecordProvider);
        requireNonNull(consumptionRecordService);
        consumptionRecordProvider.ifPresent(consumptionRecordService::registerProvider);
    }
}
