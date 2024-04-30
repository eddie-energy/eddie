package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.v0_82.EddieAccountingPointMarketDocumentProvider;
import energy.eddie.core.services.EddieAccountingPointMarketDocumentService;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The {@code EddieAccountingPointMarketDocumentServiceRegistrar} will be added to each region connector's own context
 * and will register the {@link EddieAccountingPointMarketDocumentProvider} of each region connector to the common
 * {@link EddieAccountingPointMarketDocumentService}. Nothing happens, if a certain region connector does not have a
 * {@code accountingPointMarketDocumentProvider} in its context.
 */
@RegionConnectorExtension
public class EddieAccountingPointMarketDocumentServiceRegistrar {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public EddieAccountingPointMarketDocumentServiceRegistrar(
            Optional<EddieAccountingPointMarketDocumentProvider> accountingPointMarketDocumentProvider,
            EddieAccountingPointMarketDocumentService cimService
    ) {
        requireNonNull(accountingPointMarketDocumentProvider);
        requireNonNull(cimService);
        accountingPointMarketDocumentProvider.ifPresent(cimService::registerProvider);
    }
}
