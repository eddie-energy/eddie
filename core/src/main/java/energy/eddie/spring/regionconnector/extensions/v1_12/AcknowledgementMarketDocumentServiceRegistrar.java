// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions.v1_12;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.v1_12.AcknowledgementMarketDocumentProvider;
import energy.eddie.core.services.v1_12.AcknowledgementMarketDocumentService;
import org.springframework.beans.factory.ObjectProvider;

/**
 * The {@code AcknowledgementMarketDocumentServiceRegistrar} will be added to each region connector's own context and will
 * register the {@link AcknowledgementMarketDocumentProvider} of each region connector to the common {@link AcknowledgementMarketDocumentService}.
 * Nothing happens, if a certain region connector does not have a {@code AcknowledgementMarketDocumentProvider} in its context.
 */
@RegionConnectorExtension
@SuppressWarnings("java:S101")
public class AcknowledgementMarketDocumentServiceRegistrar {
    public AcknowledgementMarketDocumentServiceRegistrar(
            ObjectProvider<AcknowledgementMarketDocumentProvider> acknowledgementEnvelopeProvider,
            AcknowledgementMarketDocumentService cimService
    ) {
        acknowledgementEnvelopeProvider.ifAvailable(cimService::registerProvider);
    }
}
