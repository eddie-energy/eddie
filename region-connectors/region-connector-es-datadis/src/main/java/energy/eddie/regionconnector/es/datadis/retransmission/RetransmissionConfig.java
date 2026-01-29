// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.retransmission;

import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.retransmission.CommonRetransmissionService;
import energy.eddie.regionconnector.shared.retransmission.RetransmissionValidation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetransmissionConfig {

    @Bean
    public RetransmissionValidation retransmissionValidation(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService) {
        return new RetransmissionValidation(
                DatadisRegionConnectorMetadata.getInstance(),
                dataNeedsService
        );
    }

    @Bean
    public CommonRetransmissionService<EsPermissionRequest> commonRetransmissionService(
            EsPermissionRequestRepository esPermissionRequestRepository,
            RetransmissionPollingService retransmissionPollingService,
            RetransmissionValidation retransmissionValidation
    ) {
        return new CommonRetransmissionService<>(
                esPermissionRequestRepository,
                retransmissionPollingService,
                retransmissionValidation
        );
    }
}
