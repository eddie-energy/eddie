// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.aspects;

import energy.eddie.regionconnector.cds.health.HealthIndicatorCreator;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.services.client.creation.responses.ApiClientCreationResponse;
import energy.eddie.regionconnector.cds.services.client.creation.responses.CreatedCdsClientResponse;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;

@Aspect
@Configurable
public class CdsServerCreationAspect {
    private static final Logger LOGGER = LoggerFactory.getLogger(CdsServerCreationAspect.class);
    private final HealthIndicatorCreator creator;

    public CdsServerCreationAspect(HealthIndicatorCreator creator) {
        this.creator = creator;
    }

    @AfterReturning(value = "execution(energy.eddie.regionconnector.cds.services.client.creation.responses.ApiClientCreationResponse createOAuthClients(..))", returning = "response")
    public void registerHealthIndicators(ApiClientCreationResponse response) {
        LOGGER.debug("Registering health indicators");
        if (response instanceof CreatedCdsClientResponse(CdsServer cdsServer)) {
            creator.register(cdsServer);
            LOGGER.debug("Successfully registered health indicators");
        }
    }
}
