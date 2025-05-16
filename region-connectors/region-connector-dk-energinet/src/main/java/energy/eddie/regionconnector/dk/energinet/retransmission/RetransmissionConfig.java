package energy.eddie.regionconnector.dk.energinet.retransmission;

import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.persistence.DkPermissionRequestRepository;
import energy.eddie.regionconnector.shared.retransmission.CommonRetransmissionService;
import energy.eddie.regionconnector.shared.retransmission.RetransmissionValidation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetransmissionConfig {

    @Bean
    public CommonRetransmissionService<DkEnerginetPermissionRequest> retransmissionService(
            DkPermissionRequestRepository dkPermissionRequestRepository,
            EnerginetPollingFunction energinetPollingFunction,
            EnerginetRegionConnector energinetRegionConnector,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService
    ) {
        return new CommonRetransmissionService<>(
                dkPermissionRequestRepository,
                energinetPollingFunction,
                new RetransmissionValidation(
                        energinetRegionConnector.getMetadata(),
                        dataNeedsService
                )
        );
    }
}
