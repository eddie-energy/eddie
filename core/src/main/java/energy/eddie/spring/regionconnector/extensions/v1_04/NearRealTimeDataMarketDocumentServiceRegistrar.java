package energy.eddie.spring.regionconnector.extensions.v1_04;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.v1_04.NearRealTimeDataMarketDocumentProvider;
import energy.eddie.core.services.v1_04.NearRealTimeDataMarketDocumentService;
import org.springframework.beans.factory.ObjectProvider;

/**
 * The {@code NearRealTimeDataEnvelopeServiceRegistrar} will be added to each region connector's own context and will
 * register the {@link NearRealTimeDataMarketDocumentProvider} of each region connector to the common {@link NearRealTimeDataMarketDocumentService}.
 * Nothing happens, if a certain region connector does not have a {@code NearRealTimeDataMarketDocumentProvider} in its context.
 */
@RegionConnectorExtension
public class NearRealTimeDataMarketDocumentServiceRegistrar {
    // TODO: add tests for this class
    public NearRealTimeDataMarketDocumentServiceRegistrar(
            ObjectProvider<NearRealTimeDataMarketDocumentProvider> NearRealTimeDataEnvelopeProvider,
            NearRealTimeDataMarketDocumentService cimService
    ) {
        NearRealTimeDataEnvelopeProvider.ifAvailable(cimService::registerProvider);
    }
}
