// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.aspects;

import energy.eddie.regionconnector.cds.health.HealthIndicatorCreator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
public class AspectConfig {

    @Bean
    public CdsServerCreationAspect cdsServerCreationAspect(HealthIndicatorCreator creator) {
        return new CdsServerCreationAspect(creator);
    }
}
