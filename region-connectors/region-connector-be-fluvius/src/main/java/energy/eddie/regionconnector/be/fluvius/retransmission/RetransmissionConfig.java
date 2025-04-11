package energy.eddie.regionconnector.be.fluvius.retransmission;

import energy.eddie.api.agnostic.retransmission.RegionConnectorRetransmissionService;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.be.fluvius.FluviusRegionConnectorMetadata;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import energy.eddie.regionconnector.shared.retransmission.CommonRetransmissionService;
import energy.eddie.regionconnector.shared.retransmission.RetransmissionValidation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetransmissionConfig {
    @Bean
    public RegionConnectorRetransmissionService retransmissionService(
            BePermissionRequestRepository bePermissionRequestRepository,
            RetransmissionPollingService retransmissionPollingService,
            RetransmissionValidation retransmissionValidation
    ) {
        return new CommonRetransmissionService<>(
                bePermissionRequestRepository,
                retransmissionPollingService,
                retransmissionValidation
        );
    }

    @Bean
    public RetransmissionValidation retransmissionValidation(
            FluviusRegionConnectorMetadata fluviusRegionConnectorMetadata,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService
    ) {
        return new RetransmissionValidation(
                fluviusRegionConnectorMetadata,
                dataNeedsService
        );
    }
}
