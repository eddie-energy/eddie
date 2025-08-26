package energy.eddie.regionconnector.us.green.button.services.retransmission;

import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.shared.retransmission.CommonRetransmissionService;
import energy.eddie.regionconnector.shared.retransmission.RetransmissionValidation;
import energy.eddie.regionconnector.us.green.button.GreenButtonRegionConnector;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetransmissionConfig {
    @Bean
    CommonRetransmissionService<UsGreenButtonPermissionRequest> bean(
            UsPermissionRequestRepository repository,
            GreenButtonPollingFunction function,
            RetransmissionValidation retransmissionValidation
    ) {
        return new CommonRetransmissionService<>(
                repository,
                function,
                retransmissionValidation
        );
    }

    @Bean
    RetransmissionValidation retransmissionValidation(
            GreenButtonRegionConnector greenButtonRegionConnector,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService
    ) {
        return new RetransmissionValidation(
                greenButtonRegionConnector.getMetadata(),
                dataNeedsService
        );
    }
}
