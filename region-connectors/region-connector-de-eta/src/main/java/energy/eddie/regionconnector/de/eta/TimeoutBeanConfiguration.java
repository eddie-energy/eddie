// SPDX-FileCopyrightText: 2026 The ETA+ Developers <bilal.sakhawat@etaplus.energy>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta;

import energy.eddie.regionconnector.de.eta.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.timeout.CommonTimeoutService;
import energy.eddie.regionconnector.shared.timeout.TimeoutConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the permission request timeout feature for the German (ETA Plus) region connector.
 *
 * <p>Final customers sometimes ignore permission requests; since those can never be used to
 * retrieve data, they are timed out by the shared {@link CommonTimeoutService}. The service runs
 * on the schedule defined by the shared {@code @Timeout} annotation (enabled by the
 * {@code @EnableScheduling} on {@link DeEtaSpringConfig}), queries stale requests via
 * {@link DePermissionRequestRepository#findStalePermissionRequests(int)} and emits the
 * corresponding timeout events through the {@link Outbox}.
 */
@Configuration
public class TimeoutBeanConfiguration {

    @Bean
    public CommonTimeoutService timeoutService(
            DePermissionRequestRepository repository,
            Outbox outbox,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            TimeoutConfiguration timeoutConfiguration
    ) {
        return new CommonTimeoutService(
                repository,
                SimpleEvent::new,
                outbox,
                timeoutConfiguration,
                EtaRegionConnectorMetadata.getInstance()
        );
    }
}