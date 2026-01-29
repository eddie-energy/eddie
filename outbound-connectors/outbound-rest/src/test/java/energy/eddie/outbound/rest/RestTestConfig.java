package energy.eddie.outbound.rest;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;

@TestConfiguration
public class RestTestConfig {
    @Bean
    public JsonMapper objectMapper() {
        var builder = JsonMapper.builder();
        new RestOutboundBeanConfig().jsonMapperBuilderCustomizer().customize(builder);
        return builder.build();
    }
}
