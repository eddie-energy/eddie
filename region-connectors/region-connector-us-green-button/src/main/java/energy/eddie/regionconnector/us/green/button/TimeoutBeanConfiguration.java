// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button;

import energy.eddie.api.agnostic.process.model.persistence.StalePermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.timeout.CommonTimeoutService;
import energy.eddie.regionconnector.shared.timeout.TimeoutConfiguration;
import energy.eddie.regionconnector.us.green.button.permission.events.UsSimpleEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeoutBeanConfiguration {
    @Bean
    public CommonTimeoutService timeoutService(
            StalePermissionRequestRepository<?> repository,
            Outbox outbox,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            TimeoutConfiguration timeoutConfiguration
    ) {
        return new CommonTimeoutService(
                repository,
                UsSimpleEvent::new,
                outbox,
                timeoutConfiguration,
                GreenButtonRegionConnectorMetadata.getInstance()
        );
    }
}
