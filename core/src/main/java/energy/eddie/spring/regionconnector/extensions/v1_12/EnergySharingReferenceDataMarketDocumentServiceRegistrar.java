// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions.v1_12;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.v1_12.EnergySharingReferenceDataMarketDocumentProvider;
import energy.eddie.core.services.v1_12.EnergySharingReferenceDataMarketDocumentService;
import org.springframework.beans.factory.ObjectProvider;

/**
 * The {@code EnergySharingReferenceDataMarketDocumentServiceRegistrar} will be added to each region connector's own context and will
 * register the {@link EnergySharingReferenceDataMarketDocumentProvider} of each region connector to the common {@link EnergySharingReferenceDataMarketDocumentService}.
 * Nothing happens, if a certain region connector does not have a {@code EnergySharingReferenceDataMarketDocumentProvider} in its context.
 */
@RegionConnectorExtension
public class EnergySharingReferenceDataMarketDocumentServiceRegistrar {
    public EnergySharingReferenceDataMarketDocumentServiceRegistrar(
            ObjectProvider<EnergySharingReferenceDataMarketDocumentProvider> provider,
            EnergySharingReferenceDataMarketDocumentService cimService
    ) {
        provider.ifAvailable(cimService::registerProvider);
    }
}
