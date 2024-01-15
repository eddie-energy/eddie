package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.v0_82.ConsentMarketDocumentProvider;
import energy.eddie.core.services.ConsentMarketDocumentService;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The {@code ConsentMarketDocumentServiceRegistrar} will be added to each region connector's own context and will
 * register the {@link ConsentMarketDocumentProvider} of each region connector to the common {@link ConsentMarketDocumentService}.
 * Nothing happens, if a certain region connector does not have a {@code CimConsentMarketDocumentProvider} in its context.
 */
@RegionConnectorExtension
public class ConsentMarketDocumentServiceRegistrar {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // Not every region connector provides an implementation
    public ConsentMarketDocumentServiceRegistrar(Optional<ConsentMarketDocumentProvider> consentMarketDocumentProvider,
                                                 ConsentMarketDocumentService consentMarketDocumentService) {
        requireNonNull(consentMarketDocumentProvider);
        requireNonNull(consentMarketDocumentService);
        consentMarketDocumentProvider.ifPresent(consentMarketDocumentService::registerProvider);
    }
}
