package energy.eddie.outbound.rest.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.outbound.rest.dto.ConnectionStatusMessages;
import energy.eddie.outbound.rest.mixins.ConnectionStatusMessageMixin;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;

@TestConfiguration
public class WebTestConfig {
    @Bean
    public RequestedContentTypeResolver requestedContentTypeResolver() {
        return new RequestedContentTypeResolverBuilder().build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .addMixIn(ConnectionStatusMessages.class, ConnectionStatusMessageMixin.class);
    }
}
