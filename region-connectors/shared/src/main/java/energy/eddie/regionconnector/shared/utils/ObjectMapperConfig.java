package energy.eddie.regionconnector.shared.utils;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class ObjectMapperConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                         .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                         .build();
    }
}
