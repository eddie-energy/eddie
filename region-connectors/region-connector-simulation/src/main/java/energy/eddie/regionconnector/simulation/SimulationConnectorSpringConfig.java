// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.regionconnector.shared.utils.CommonPaths;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import reactor.core.publisher.Sinks;

import static energy.eddie.regionconnector.simulation.SimulationConnectorMetadata.REGION_CONNECTOR_ID;

@EnableWebMvc
@SpringBootApplication
@energy.eddie.api.agnostic.RegionConnector(name = REGION_CONNECTOR_ID)
public class SimulationConnectorSpringConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        var classPathLocation = "classpath:/public/%s/%s/".formatted(CommonPaths.ALL_REGION_CONNECTORS_BASE_URL_PATH,
                                                                     REGION_CONNECTOR_ID);
        // add a resource handler that serves all public files of this region connector
        registry.addResourceHandler("/**")
                .addResourceLocations(classPathLocation);
    }

    @Bean
    public Sinks.Many<ConnectionStatusMessage> connectionStatusStreamSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }
}
