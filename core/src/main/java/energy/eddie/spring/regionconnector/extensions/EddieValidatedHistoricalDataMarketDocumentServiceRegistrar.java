package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.v0_82.CimConsumptionRecordProvider;
import energy.eddie.core.services.EddieValidatedHistoricalDataMarketDocumentService;
import energy.eddie.spring.RegionConnectorExtension;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The {@code EddieValidatedHistoricalDataMarketDocumentServiceRegistrar} will be added to each region connector's own context and will
 * register the {@link CimConsumptionRecordProvider} of each region connector to the common {@link EddieValidatedHistoricalDataMarketDocumentService}.
 * Nothing happens, if a certain region connector does not have a {@code CimConsumptionRecordProvider} in its context.
 */
@RegionConnectorExtension
public class EddieValidatedHistoricalDataMarketDocumentServiceRegistrar {
    public EddieValidatedHistoricalDataMarketDocumentServiceRegistrar(Optional<CimConsumptionRecordProvider> consumptionRecordProvider,
                                                                      EddieValidatedHistoricalDataMarketDocumentService cimService) {
        requireNonNull(consumptionRecordProvider);
        requireNonNull(cimService);
        consumptionRecordProvider.ifPresent(cimService::registerProvider);
    }
}
