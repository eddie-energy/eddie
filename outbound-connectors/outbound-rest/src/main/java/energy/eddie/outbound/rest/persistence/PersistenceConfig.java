package energy.eddie.outbound.rest.persistence;

import energy.eddie.outbound.shared.hibernate.Jackson3JsonFormatMapper;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableJpaRepositories
public class PersistenceConfig {
    @Bean
    HibernatePropertiesCustomizer hibernatePropertiesCustomizer(ObjectMapper objectMapper) {
        return props -> props.put(
                "hibernate.type.json_format_mapper",
                new Jackson3JsonFormatMapper(objectMapper)
        );
    }
}
