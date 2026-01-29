// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions.v0_82;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.v0_82.ValidatedHistoricalDataEnvelopeProvider;
import energy.eddie.core.services.v0_82.ValidatedHistoricalDataEnvelopeService;

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
            Optional<ValidatedHistoricalDataEnvelopeProvider> validatedHistoricalDataEnvelopeProvider,
            ValidatedHistoricalDataEnvelopeService cimService
    ) {
        requireNonNull(validatedHistoricalDataEnvelopeProvider);
        requireNonNull(cimService);
        validatedHistoricalDataEnvelopeProvider.ifPresent(cimService::registerProvider);
    }
}
