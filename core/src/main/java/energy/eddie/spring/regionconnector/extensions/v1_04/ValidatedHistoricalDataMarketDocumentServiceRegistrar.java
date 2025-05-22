package energy.eddie.spring.regionconnector.extensions.v1_04;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.v1_04.ValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.core.services.v1_04.ValidatedHistoricalDataMarketDocumentService;
import org.springframework.beans.factory.ObjectProvider;

/**
 * The {@code ValidatedHistoricalDataEnvelopeServiceRegistrar} will be added to each region connector's own context and will
 * register the {@link ValidatedHistoricalDataMarketDocumentProvider} of each region connector to the common {@link ValidatedHistoricalDataMarketDocumentService}.
 * Nothing happens, if a certain region connector does not have a {@code ValidatedHistoricalDataMarketDocumentProvider} in its context.
 */
@RegionConnectorExtension
public class ValidatedHistoricalDataMarketDocumentServiceRegistrar {
    public ValidatedHistoricalDataMarketDocumentServiceRegistrar(
            ObjectProvider<ValidatedHistoricalDataMarketDocumentProvider> validatedHistoricalDataEnvelopeProvider,
            ValidatedHistoricalDataMarketDocumentService cimService
    ) {
        validatedHistoricalDataEnvelopeProvider.ifAvailable(cimService::registerProvider);
    }
}
