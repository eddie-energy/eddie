package energy.eddie.core;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

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
    public HibernatePropertiesCustomizer jsonFormatMapperCustomizer(JsonMapper objectMapper) {
        // TODO: Switch to jackson3 object mapper
        return props -> props.put(MappingSettings.JSON_FORMAT_MAPPER, new JacksonJsonFormatMapper());
    }
}
