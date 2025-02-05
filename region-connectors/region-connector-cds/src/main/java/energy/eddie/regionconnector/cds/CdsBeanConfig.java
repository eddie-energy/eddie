package energy.eddie.regionconnector.cds;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.regionconnector.cds.config.CdsConfiguration;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(value = CdsConfiguration.class)
public class CdsBeanConfig {
    @Bean
    public EventBus eventBus() {
        return new EventBusImpl();
    }

    @Bean
    @Primary
    public Jackson2ObjectMapperBuilder customObjectMapper() {
        return new Jackson2ObjectMapperBuilder()
                .modules(new JsonNullableModule(), new Jdk8Module(), new JavaTimeModule());
    }

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }
}
