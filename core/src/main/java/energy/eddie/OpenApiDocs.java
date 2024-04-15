package energy.eddie;

import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static energy.eddie.regionconnector.shared.utils.CommonPaths.ALL_REGION_CONNECTORS_BASE_URL_PATH;
import static energy.eddie.spring.RegionConnectorRegistrationBeanPostProcessor.ENABLED_REGION_CONNECTOR_BEAN_NAME;

/**
 * Adds the OpenAPI documentation URLs of all enabled region connectors to the OpenAPI documentation in the current
 * context, so that they can be accessed in the "Select a definition" drop down menu.
 * Additionally, it also adds the API documentation for the European MasterData.
 */
public class OpenApiDocs {
    @Bean
    @Primary
    public SwaggerUiConfigProperties swaggerUiConfig(
            SwaggerUiConfigProperties config,
            @Value("${server.port}") int serverPort,
            @Qualifier(ENABLED_REGION_CONNECTOR_BEAN_NAME) List<String> enabledRegionConnectors
    ) {
        final String docPath = "v3/api-docs";

        var swaggerUrls = enabledRegionConnectors
                .stream()
                .map(name -> {
                    // CVE-2024-22243 can be ignored, as we are not parsing an externally provided URL
                    String docUrl = UriComponentsBuilder
                            .newInstance()
                            .scheme("http")
                            .host("localhost")
                            .port(serverPort)
                            .pathSegment(ALL_REGION_CONNECTORS_BASE_URL_PATH)
                            .pathSegment(name)
                            .path(docPath)
                            .toUriString();

                    return new AbstractSwaggerUiConfigProperties.SwaggerUrl("region-connector-" + name,
                                                                            docUrl,
                                                                            name.toUpperCase(Locale.ENGLISH));
                })
                .collect(Collectors.toSet());

        swaggerUrls.add(getEuropeanMasterDataSwaggerUrl(serverPort, docPath));
        config.setUrls(swaggerUrls);
        return config;
    }

    private static AbstractSwaggerUiConfigProperties.SwaggerUrl getEuropeanMasterDataSwaggerUrl(
            int serverPort,
            String docPath
    ) {
        var url = UriComponentsBuilder
                .newInstance()
                .scheme("http")
                .host("localhost")
                .port(serverPort)
                .pathSegment("european-masterdata")
                .path(docPath)
                .toUriString();

        return new AbstractSwaggerUiConfigProperties.SwaggerUrl("european-masterdata",
                                                                url,
                                                                "European Master Data API");
    }
}
