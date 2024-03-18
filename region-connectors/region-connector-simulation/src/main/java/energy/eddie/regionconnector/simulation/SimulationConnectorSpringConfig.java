package energy.eddie.regionconnector.simulation;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import reactor.core.publisher.Sinks;

import static energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration.ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY;
import static energy.eddie.regionconnector.simulation.SimulationConnectorMetadata.REGION_CONNECTOR_ID;

@EnableWebMvc
@SpringBootApplication
@energy.eddie.api.agnostic.RegionConnector(name = REGION_CONNECTOR_ID)
public class SimulationConnectorSpringConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        var classPathLocation = "classpath:/public/region-connectors/%s/".formatted(REGION_CONNECTOR_ID);
        // add a resource handler that serves all public files of this region connector
        registry.addResourceHandler("/**")
                .addResourceLocations(classPathLocation);
    }

    @Bean
    public Sinks.Many<ConnectionStatusMessage> connectionStatusStreamSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public CommonInformationModelConfiguration commonInformationModelConfiguration(
            @Value("${" + ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY + "}") String codingSchemeTypeList) {
        return new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.fromValue(codingSchemeTypeList));
    }
}
