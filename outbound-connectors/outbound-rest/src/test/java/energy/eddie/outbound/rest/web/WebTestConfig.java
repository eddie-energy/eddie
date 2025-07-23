package energy.eddie.outbound.rest.web;

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
}
