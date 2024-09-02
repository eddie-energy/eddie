package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.v0_82.PermissionMarketDocumentProvider;
import energy.eddie.core.services.PermissionMarketDocumentService;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The {@code PermissionMarketDocumentServiceRegistrar} will be added to each region connector's own context and will
 * register the {@link PermissionMarketDocumentProvider} of each region connector to the common
 * {@link PermissionMarketDocumentService}. Nothing happens, if a certain region connector does not have a
 * {@code CimPermissionMarketDocumentProvider} in its context.
 */
@RegionConnectorExtension
public class PermissionMarketDocumentServiceRegistrar {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // Not every region connector provides an implementation
    public PermissionMarketDocumentServiceRegistrar(
            Optional<PermissionMarketDocumentProvider> permissionMarketDocumentProvider,
            PermissionMarketDocumentService permissionMarketDocumentService
    ) {
        requireNonNull(permissionMarketDocumentProvider);
        requireNonNull(permissionMarketDocumentService);
        permissionMarketDocumentProvider.ifPresent(permissionMarketDocumentService::registerProvider);
    }
}
