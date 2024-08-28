package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.v0_82.AccountingPointEnveloppeProvider;
import energy.eddie.core.services.AccountingPointEnveloppeService;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The {@code AccountingPointEnveloppeServiceRegistrar} will be added to each region connector's own context
 * and will register the {@link AccountingPointEnveloppeProvider} of each region connector to the common
 * {@link AccountingPointEnveloppeService}. Nothing happens, if a certain region connector does not have a
 * {@code accountingPointMarketDocumentProvider} in its context.
 */
@RegionConnectorExtension
public class AccountingPointEnveloppeServiceRegistrar {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public AccountingPointEnveloppeServiceRegistrar(
            Optional<AccountingPointEnveloppeProvider> accountingPointMarketDocumentProvider,
            AccountingPointEnveloppeService cimService
    ) {
        requireNonNull(accountingPointMarketDocumentProvider);
        requireNonNull(cimService);
        accountingPointMarketDocumentProvider.ifPresent(cimService::registerProvider);
    }
}
