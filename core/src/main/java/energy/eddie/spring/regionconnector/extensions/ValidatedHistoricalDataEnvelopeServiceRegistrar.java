package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.v0_82.ValidatedHistoricalDataEnvelopeProvider;
import energy.eddie.core.services.ValidatedHistoricalDataEnvelopeService;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The {@code EddieValidatedHistoricalDataMarketDocumentServiceRegistrar} will be added to each region connector's own context and will
 * register the {@link ValidatedHistoricalDataEnvelopeProvider} of each region connector to the common {@link ValidatedHistoricalDataEnvelopeService}.
 * Nothing happens, if a certain region connector does not have a {@code CimConsumptionRecordProvider} in its context.
 */
@RegionConnectorExtension
public class ValidatedHistoricalDataEnvelopeServiceRegistrar {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public ValidatedHistoricalDataEnvelopeServiceRegistrar(
            Optional<ValidatedHistoricalDataEnvelopeProvider> consumptionRecordProvider,
            ValidatedHistoricalDataEnvelopeService cimService
    ) {
        requireNonNull(consumptionRecordProvider);
        requireNonNull(cimService);
        consumptionRecordProvider.ifPresent(cimService::registerProvider);
    }
}