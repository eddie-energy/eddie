package energy.eddie.spring.regionconnector.extensions.v0_92;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.v0_92.NearRealTimeMeasurementMarketDocumentProvider;
import energy.eddie.core.services.v0_92.NearRealTimeMeasurementMarketDocumentService;
import org.springframework.beans.factory.ObjectProvider;

/**
 * The {@link NearRealTimeMeasurementMarketDocumentServiceRegistrar} will be added to each region connector's own context and will
 * register the {@link energy.eddie.api.v0_92.NearRealTimeMeasurementMarketDocumentProvider} of each region connector to the common {@link energy.eddie.core.services.v0_92.NearRealTimeMeasurementMarketDocumentService}.
 * Nothing happens, if a certain region connector does not have a {@link energy.eddie.api.v0_92.NearRealTimeMeasurementMarketDocumentProvider} in its context.
 */
@RegionConnectorExtension
public class NearRealTimeMeasurementMarketDocumentServiceRegistrar {
    public NearRealTimeMeasurementMarketDocumentServiceRegistrar(
            ObjectProvider<NearRealTimeMeasurementMarketDocumentProvider> provider,
            NearRealTimeMeasurementMarketDocumentService cimService
    ) {
        provider.ifAvailable(cimService::registerProvider);
    }
}
