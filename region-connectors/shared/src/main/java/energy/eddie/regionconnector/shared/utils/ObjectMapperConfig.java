// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.utils;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * This configuration should be used to override the object mapper that is passed to the child instances from the core parent context.
 * It has similar features to the {@link org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration}, but is not as sophisticated.
 */
@Configuration
public class ObjectMapperConfig {
    @Bean
    @Primary
    public JsonMapper jsonMapper(List<JsonMapperBuilderCustomizer> customizers) {
        var builder = JsonMapper.builder();
        customizers.forEach(customizer -> customizer.customize(builder));
        return builder.build();
    }
}
