package energy.eddie.dataneeds.web;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.DeserializationFeature;

@TestConfiguration
public class WebTestConfig {
    @Bean
    public JsonMapperBuilderCustomizer customizer() {
        return builder -> builder.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
    }
}
