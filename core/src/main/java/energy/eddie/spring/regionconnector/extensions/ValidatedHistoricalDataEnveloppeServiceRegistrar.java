package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.v0_82.ValidatedHistoricalDataEnveloppeProvider;
import energy.eddie.core.services.ValidatedHistoricalDataEnveloppeService;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The {@code EddieValidatedHistoricalDataMarketDocumentServiceRegistrar} will be added to each region connector's own context and will
 * register the {@link ValidatedHistoricalDataEnveloppeProvider} of each region connector to the common {@link ValidatedHistoricalDataEnveloppeService}.
 * Nothing happens, if a certain region connector does not have a {@code CimConsumptionRecordProvider} in its context.
 */
@RegionConnectorExtension
public class ValidatedHistoricalDataEnveloppeServiceRegistrar {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public ValidatedHistoricalDataEnveloppeServiceRegistrar(
            Optional<ValidatedHistoricalDataEnveloppeProvider> consumptionRecordProvider,
            ValidatedHistoricalDataEnveloppeService cimService
    ) {
        requireNonNull(consumptionRecordProvider);
        requireNonNull(cimService);
        consumptionRecordProvider.ifPresent(cimService::registerProvider);
    }
}