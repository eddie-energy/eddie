// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest;

import energy.eddie.outbound.rest.config.RestOutboundConnectorConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.boot.jackson.autoconfigure.XmlMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;

import java.util.List;

@Configuration
@EnableConfigurationProperties(RestOutboundConnectorConfiguration.class)
public class RestOutboundBeanConfig {
    @Bean
    @Primary
    public JsonMapper jsonMapper(List<JsonMapperBuilderCustomizer> customizers) {
        var builder = JsonMapper.builder();
        customizers.forEach(customizer -> customizer.customize(builder));
        return builder.build();
    }

    @Bean
    public XmlMapperBuilderCustomizer xmlMapperBuilderCustomizer() {
        return RestOutboundBeanConfig::builderCustomizer;
    }

    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        return RestOutboundBeanConfig::builderCustomizer;
    }

    private static <M extends ObjectMapper, B extends MapperBuilder<M, B>> void builderCustomizer(MapperBuilder<M, B> builder) {
        builder
                .addModule(new JakartaXmlBindAnnotationModule());
    }
}
