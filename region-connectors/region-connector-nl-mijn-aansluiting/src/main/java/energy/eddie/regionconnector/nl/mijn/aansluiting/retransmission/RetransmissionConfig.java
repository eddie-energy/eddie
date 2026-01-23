// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.retransmission;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.persistence.NlPermissionRequestRepository;
import energy.eddie.regionconnector.shared.retransmission.CommonRetransmissionService;
import energy.eddie.regionconnector.shared.retransmission.PollingFunction;
import energy.eddie.regionconnector.shared.retransmission.RetransmissionValidation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetransmissionConfig {
    @Bean
    public CommonRetransmissionService<MijnAansluitingPermissionRequest> retransmissionService(
            NlPermissionRequestRepository nlPermissionRequestRepository,
            RegionConnector regionConnector,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService,
            PollingFunction<MijnAansluitingPermissionRequest> mijnAansluitingPollingFunction
    ) {
        return new CommonRetransmissionService<>(
               nlPermissionRequestRepository,
               mijnAansluitingPollingFunction,
               new RetransmissionValidation(
                       regionConnector.getMetadata(),
                       dataNeedsService
               )
        );
    }
}
