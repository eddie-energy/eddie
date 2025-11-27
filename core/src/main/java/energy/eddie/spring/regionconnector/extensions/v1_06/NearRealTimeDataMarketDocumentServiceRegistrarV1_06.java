package energy.eddie.spring.regionconnector.extensions.v1_06;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.v1_06.NearRealTimeDataMarketDocumentProviderV1_06;
import energy.eddie.core.services.v1_06.NearRealTimeDataMarketDocumentService;
import org.springframework.beans.factory.ObjectProvider;

/**
 * The {@code NearRealTimeDataEnvelopeServiceRegistrar} will be added to each region connector's own context and will
 * register the {@link NearRealTimeDataMarketDocumentProviderV1_06} of each region connector to the common {@link NearRealTimeDataMarketDocumentService}.
 * Nothing happens, if a certain region connector does not have a {@code NearRealTimeDataMarketDocumentProvider} in its context.
 */
@RegionConnectorExtension
@SuppressWarnings("java:S101")
public class NearRealTimeDataMarketDocumentServiceRegistrarV1_06 {
    public NearRealTimeDataMarketDocumentServiceRegistrarV1_06(
            ObjectProvider<NearRealTimeDataMarketDocumentProviderV1_06> nearRealTimeDataEnvelopeProvider,
            NearRealTimeDataMarketDocumentService cimService
    ) {
        nearRealTimeDataEnvelopeProvider.ifAvailable(cimService::registerProvider);
    }
}
