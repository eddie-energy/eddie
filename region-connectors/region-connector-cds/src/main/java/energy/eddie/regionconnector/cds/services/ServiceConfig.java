// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.services;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.cds.CdsRegionConnectorMetadata;
import energy.eddie.regionconnector.cds.permission.events.SimpleEvent;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import energy.eddie.regionconnector.cds.persistence.CdsPermissionRequestRepository;
import energy.eddie.regionconnector.cds.services.retransmission.RetransmissionPollingService;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.retransmission.CommonRetransmissionService;
import energy.eddie.regionconnector.shared.retransmission.RetransmissionValidation;
import energy.eddie.regionconnector.shared.services.CommonFutureDataService;
import energy.eddie.regionconnector.shared.timeout.CommonTimeoutService;
import energy.eddie.regionconnector.shared.timeout.TimeoutConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class ServiceConfig {
    @Bean
    public CommonTimeoutService commonTimeoutService(
            CdsPermissionRequestRepository cdsPermissionRequestRepository,
            Outbox outbox,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            TimeoutConfiguration timeoutConfiguration,
            CdsRegionConnectorMetadata cdsRegionConnectorMetadata
    ) {
        return new CommonTimeoutService(
                cdsPermissionRequestRepository,
                SimpleEvent::new,
                outbox,
                timeoutConfiguration,
                cdsRegionConnectorMetadata
        );
    }

    @Bean
    public CommonFutureDataService<CdsPermissionRequest> futureDataService(
            PollingService pollingService,
            CdsPermissionRequestRepository cdsPermissionRequestRepository,
            @Value("${region-connector.cds.polling:0 0 17 * * *}") String cronExpr,
            CdsRegionConnectorMetadata cdsRegionConnectorMetadata,
            TaskScheduler taskScheduler,
            DataNeedCalculationService<DataNeed> dataNeedCalculationService
    ) {
        return new CommonFutureDataService<>(
                pollingService,
                cdsPermissionRequestRepository,
                cronExpr,
                cdsRegionConnectorMetadata,
                taskScheduler,
                dataNeedCalculationService
        );
    }

    @Bean
    public CommonRetransmissionService<CdsPermissionRequest> retransmissionService(
            CdsPermissionRequestRepository cdsPermissionRequestRepository,
            CdsRegionConnectorMetadata cdsRegionConnectorMetadata,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService,
            RetransmissionPollingService retransmissionPollingService
    ) {
        return new CommonRetransmissionService<>(
                cdsPermissionRequestRepository,
                retransmissionPollingService,
                new RetransmissionValidation(
                        cdsRegionConnectorMetadata,
                        dataNeedsService
                )
        );
    }
}
