package energy.eddie.spring.rcprocessors;

import energy.eddie.api.v0_82.CimConsumptionRecordProvider;
import energy.eddie.core.services.EddieValidatedHistoricalDataMarketDocumentService;
import energy.eddie.spring.RegionConnectorProcessor;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

@RegionConnectorProcessor
public class EddieValidatedHistoricalDataMarketDocumentServiceRegistrar {
    public EddieValidatedHistoricalDataMarketDocumentServiceRegistrar(Optional<CimConsumptionRecordProvider> consumptionRecordProvider,
                                                                      EddieValidatedHistoricalDataMarketDocumentService cimService) {
        requireNonNull(consumptionRecordProvider);
        requireNonNull(cimService);
        consumptionRecordProvider.ifPresent(cimService::registerProvider);
    }
}
