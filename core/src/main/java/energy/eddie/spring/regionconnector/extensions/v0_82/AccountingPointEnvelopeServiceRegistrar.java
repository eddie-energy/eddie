package energy.eddie.spring.regionconnector.extensions.v0_82;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.v0_82.AccountingPointEnvelopeProvider;
import energy.eddie.core.services.v0_82.AccountingPointEnvelopeService;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The {@code AccountingPointEnvelopeServiceRegistrar} will be added to each region connector's own context and will
 * register the {@link AccountingPointEnvelopeProvider} of each region connector to the common
 * {@link AccountingPointEnvelopeService}. Nothing happens, if a certain region connector does not have a
 * {@code accountingPointMarketDocumentProvider} in its context.
 */
@RegionConnectorExtension
public class AccountingPointEnvelopeServiceRegistrar {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public AccountingPointEnvelopeServiceRegistrar(
            Optional<AccountingPointEnvelopeProvider> accountingPointMarketDocumentProvider,
            AccountingPointEnvelopeService cimService
    ) {
        requireNonNull(accountingPointMarketDocumentProvider);
        requireNonNull(cimService);
        accountingPointMarketDocumentProvider.ifPresent(cimService::registerProvider);
    }
}
