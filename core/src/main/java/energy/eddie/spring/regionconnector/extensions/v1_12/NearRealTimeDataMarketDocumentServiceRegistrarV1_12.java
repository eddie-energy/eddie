// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions.v1_12;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.v1_12.NearRealTimeDataMarketDocumentProviderV1_12;
import energy.eddie.core.services.v1_12.NearRealTimeDataMarketDocumentService;
import org.springframework.beans.factory.ObjectProvider;

/**
 * The {@code NearRealTimeDataEnvelopeServiceRegistrar} will be added to each region connector's own context and will
 * register the {@link NearRealTimeDataMarketDocumentProviderV1_12} of each region connector to the common {@link NearRealTimeDataMarketDocumentService}.
 * Nothing happens, if a certain region connector does not have a {@code NearRealTimeDataMarketDocumentProvider} in its context.
 */
@RegionConnectorExtension
@SuppressWarnings("java:S101")
public class NearRealTimeDataMarketDocumentServiceRegistrarV1_12 {
    public NearRealTimeDataMarketDocumentServiceRegistrarV1_12(
            ObjectProvider<NearRealTimeDataMarketDocumentProviderV1_12> nearRealTimeDataEnvelopeProvider,
            NearRealTimeDataMarketDocumentService cimService
    ) {
        nearRealTimeDataEnvelopeProvider.ifAvailable(cimService::registerProvider);
    }
}
