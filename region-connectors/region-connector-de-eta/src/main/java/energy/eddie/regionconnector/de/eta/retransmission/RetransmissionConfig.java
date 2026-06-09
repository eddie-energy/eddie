// SPDX-FileCopyrightText: 2026 The ETA+ Developers <bilal.sakhawat@etaplus.energy>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.retransmission;

import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.de.eta.EtaRegionConnectorMetadata;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionRequestRepository;
import energy.eddie.regionconnector.shared.retransmission.CommonRetransmissionService;
import energy.eddie.regionconnector.shared.retransmission.RetransmissionValidation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the retransmission feature for the German (ETA Plus) region connector.
 *
 * <p>The {@link CommonRetransmissionService} bean is auto-discovered by the core
 * {@code RetransmissionRouterRegistrar}, which registers it with the retransmission router and
 * makes the connector advertise support for retransmission requests.
 */
@Configuration
public class RetransmissionConfig {

    @Bean
    public CommonRetransmissionService<DePermissionRequest> retransmissionService(
            DePermissionRequestRepository repository,
            EtaRetransmissionPollingFunction pollingFunction,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService
    ) {
        return new CommonRetransmissionService<>(
                repository,
                pollingFunction,
                new RetransmissionValidation(EtaRegionConnectorMetadata.getInstance(), dataNeedsService)
        );
    }
}