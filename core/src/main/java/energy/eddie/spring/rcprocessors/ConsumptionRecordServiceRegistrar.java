package energy.eddie.spring.rcprocessors;

import energy.eddie.api.v0.Mvp1ConsumptionRecordProvider;
import energy.eddie.core.services.ConsumptionRecordService;
import energy.eddie.spring.RegionConnectorProcessor;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

@RegionConnectorProcessor
public class ConsumptionRecordServiceRegistrar {
    public ConsumptionRecordServiceRegistrar(Optional<Mvp1ConsumptionRecordProvider> consumptionRecordProvider,
                                             ConsumptionRecordService consumptionRecordService) {
        requireNonNull(consumptionRecordProvider);
        requireNonNull(consumptionRecordService);
        consumptionRecordProvider.ifPresent(consumptionRecordService::registerProvider);
    }
}
