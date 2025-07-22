package energy.eddie.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// The empty persistence config is needed, so it is not loaded during testing.
// Even when excluding configs, spring boot tests load the main application config
// EntityScan with base package is required so that the JPA repositories in child context will be initialized correctly
@EntityScan(basePackages = "energy.eddie")
@Configuration
public class PersistenceConfig {

    /**
     * Sets the hibernate internal object mapper to the spring object mapper.
     * This allows customizing modules for serialization and deserialization.
     *
     * @param objectMapper Spring configured ObjectMapper
     * @return a customized hibernate config
     * @see MapperConfig#jsonCustomizer()
     */
    @Bean
    public HibernatePropertiesCustomizer jsonFormatMapperCustomizer(ObjectMapper objectMapper) {
        return props -> props.put(MappingSettings.JSON_FORMAT_MAPPER, new JacksonJsonFormatMapper(objectMapper));
    }
}
