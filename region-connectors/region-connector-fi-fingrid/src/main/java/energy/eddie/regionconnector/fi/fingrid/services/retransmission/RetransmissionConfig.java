// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.services.retransmission;

import energy.eddie.api.agnostic.retransmission.RegionConnectorRetransmissionService;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.fi.fingrid.FingridRegionConnector;
import energy.eddie.regionconnector.fi.fingrid.persistence.FiPermissionRequestRepository;
import energy.eddie.regionconnector.shared.retransmission.CommonRetransmissionService;
import energy.eddie.regionconnector.shared.retransmission.RetransmissionValidation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetransmissionConfig {

    @Bean
    public RegionConnectorRetransmissionService retransmissionService(
            FiPermissionRequestRepository repository,
            RetransmissionPollingService retransmissionPollingService,
            RetransmissionValidation retransmissionValidation
    ) {
        return new CommonRetransmissionService<>(repository, retransmissionPollingService, retransmissionValidation);
    }

    @Bean
    public RetransmissionValidation retransmissionValidation(
            FingridRegionConnector regionConnector,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService
    ) {
        return new RetransmissionValidation(regionConnector.getMetadata(), dataNeedsService);
    }
}
