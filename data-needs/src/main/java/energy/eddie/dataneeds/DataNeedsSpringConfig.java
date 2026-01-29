// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds;

import energy.eddie.dataneeds.duration.AbsoluteDuration;
import energy.eddie.dataneeds.duration.DataNeedDuration;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.InboundAiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.OutboundAiidaDataNeed;
import energy.eddie.dataneeds.services.DataNeedsDbService;
import energy.eddie.dataneeds.web.management.DataNeedsManagementController;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.util.List;
import java.util.Map;

@Configuration
@SpringBootApplication
public class DataNeedsSpringConfig {
    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

    /**
     * Clock needed by e.g. {@link energy.eddie.dataneeds.validation.duration.IsValidRelativeDurationValidator}.
     *
     * @return UTC clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        return builder -> builder.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
    }

    @Bean
    @Primary
    public JsonMapper jsonMapper(List<JsonMapperBuilderCustomizer> customizers) {
        var builder = JsonMapper.builder();
        customizers.forEach(customizer -> customizer.customize(builder));
        return builder.build();
    }

    /**
     * Returns the GroupedOpenApi definition for the data needs management API with the management port and URL prefix set.
     */
    // cannot get typed variables from OpenAPI
    @SuppressWarnings({"java:S3740", "rawtypes"})
    @Bean
    @ConditionalOnBean(value = DataNeedsDbService.class)
    public GroupedOpenApi dataNeedsManagementApi(@Value("${eddie.management.server.port}") int managementPort) {
        return GroupedOpenApi
                .builder()
                .group("data-needs-management")
                .displayName("Data needs management API")
                .packagesToScan(DataNeed.class.getPackageName(),
                                DataNeedDuration.class.getPackageName(),
                                DataNeedsManagementController.class.getPackageName())
                .addOpenApiCustomizer(openApi -> {
                    // the URL and port of the management API is different --> reflect that in the OpenAPI documentation
                    Server server = openApi.getServers().getFirst();
                    URI uri = URI.create(server.getUrl());
                    try {
                        server.url(new URI(uri.getScheme(),
                                           null,
                                           uri.getHost(),
                                           managementPort,
                                           uri.getPath(),
                                           null,
                                           null).toString());
                    } catch (URISyntaxException e) {
                        throw new BeanInitializationException("Cannot create data needs management OpenAPI definition",
                                                              e);
                    }
                    Map<String, Schema> schemas = openApi
                            .getComponents()
                            .getSchemas();
                    addDefaultValues(schemas);
                })
                .build();
    }

    /**
     * Returns the GroupedOpenApi definition for the data needs API.
     */
    // cannot get typed variables from OpenAPI
    @SuppressWarnings({"java:S3740", "rawtypes"})
    @Bean
    public GroupedOpenApi dataNeedsApi() {
        return GroupedOpenApi
                .builder()
                .group("data-needs-controller")
                .displayName("Data needs public API")
                .packagesToScan("energy.eddie.dataneeds")
                .packagesToExclude(DataNeedsManagementController.class.getPackageName())
                .addOpenApiCustomizer(openApi -> {
                    Map<String, Schema> schemas = openApi
                            .getComponents()
                            .getSchemas();
                    addDefaultValues(schemas);
                })
                .build();
    }

    /**
     * Adds the {@code type} field to {@link AccountingPointDataNeed}, {@link ValidatedHistoricalDataDataNeed},
     * {@link AiidaDataNeed}, {@link AbsoluteDuration}, {@link RelativeDuration}
     * so that it properly shows up in the OpenAPI documentation. Sets the default value of the {@code type} field to
     * the discriminator value of the class, e.g. {@link AccountingPointDataNeed#DISCRIMINATOR_VALUE}, so that the user
     * knows what to use as discriminator values.
     *
     * @param schemas Schemas of the OpenApi instance.
     */
    // cannot get typed variables from OpenAPI
    @SuppressWarnings({"java:S3740", "rawtypes"})
    private void addDefaultValues(Map<String, Schema> schemas) {
        // the "type" field is used as discriminator, however, swagger does not show the mappings by default,
        // so the user has no idea what values are permitted for "type"
        // we also do not want to add the "type" as property in the classes, so we programmatically add it
        // to the OpenAPI definition and set the default value
        addTypeWithDefaultValueToSchema(schemas,
                                        AccountingPointDataNeed.class,
                                        AccountingPointDataNeed.DISCRIMINATOR_VALUE);
        addTypeWithDefaultValueToSchema(schemas,
                                        ValidatedHistoricalDataDataNeed.class,
                                        ValidatedHistoricalDataDataNeed.DISCRIMINATOR_VALUE);
        addTypeWithDefaultValueToSchema(schemas,
                                        InboundAiidaDataNeed.class,
                                        InboundAiidaDataNeed.DISCRIMINATOR_VALUE);
        addTypeWithDefaultValueToSchema(schemas,
                                        OutboundAiidaDataNeed.class,
                                        OutboundAiidaDataNeed.DISCRIMINATOR_VALUE);
        addTypeWithDefaultValueToSchema(schemas,
                                        AbsoluteDuration.class,
                                        AbsoluteDuration.DISCRIMINATOR_VALUE);
        addTypeWithDefaultValueToSchema(schemas,
                                        RelativeDuration.class,
                                        RelativeDuration.DISCRIMINATOR_VALUE);
    }

    // cannot get typed variables from OpenAPI
    @SuppressWarnings({"java:S3740", "rawtypes", "unchecked"})
    private void addTypeWithDefaultValueToSchema(
            Map<String, Schema> schemas,
            Class<?> clazz,
            String defaultValue
    ) {
        Schema schema = schemas.get(clazz.getSimpleName());
        if (schema != null) {
            var properties = schema.getProperties();
            if (properties != null)
                properties.put("type", new StringSchema()._default(defaultValue));
            else
                schema.properties(Map.of("type", new StringSchema()._default(defaultValue)));
            schema.addRequiredItem("type");
        }
    }
}
