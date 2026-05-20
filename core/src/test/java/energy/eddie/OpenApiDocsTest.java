// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie;

import org.junit.jupiter.api.Test;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiDocsTest {

    @Test
    void testSwaggerUiConfig_withEmptyConnectors() {
        // Given
        String publicURL = "https://public.example.com";
        String managementURL = "https://management.example.com";
        SwaggerUiConfigProperties config = new SwaggerUiConfigProperties();
        OpenApiDocs openApiDocs = new OpenApiDocs();

        // When
        SwaggerUiConfigProperties result = openApiDocs.swaggerUiConfig(
                config,
                publicURL,
                managementURL,
                List.of(),
                List.of()
        );

        // Then
        Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> urls = result.getUrls();
        assertThat(urls)
                .hasSize(1)
                .anyMatch(url -> url.getName().equals("european-masterdata"));
    }

    @Test
    void testSwaggerUiConfig_withRegionConnectors() {
        // Given
        String publicURL = "https://public.example.com";
        String managementURL = "https://management.example.com";
        SwaggerUiConfigProperties config = new SwaggerUiConfigProperties();
        OpenApiDocs openApiDocs = new OpenApiDocs();

        // When
        SwaggerUiConfigProperties result = openApiDocs.swaggerUiConfig(
                config,
                publicURL,
                managementURL,
                List.of("region1", "region2"),
                List.of()
        );

        // Then
        Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> urls = result.getUrls();
        assertThat(urls)
                .hasSize(3) // Includes European Master Data + 2 region connectors
                .anyMatch(url -> url.getName().equals("region-connector-region1"))
                .anyMatch(url -> url.getName().equals("region-connector-region2"))
                .anyMatch(url -> url.getName().equals("european-masterdata"));
    }

    @Test
    void testSwaggerUiConfig_withOutboundConnectors() {
        // Given
        String publicURL = "https://public.example.com";
        String managementURL = "https://management.example.com";
        SwaggerUiConfigProperties config = new SwaggerUiConfigProperties();
        OpenApiDocs openApiDocs = new OpenApiDocs();

        // When
        SwaggerUiConfigProperties result = openApiDocs.swaggerUiConfig(
                config,
                publicURL,
                managementURL,
                List.of(),
                List.of("connector1", "connector2")
        );

        // Then
        Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> urls = result.getUrls();
        assertThat(urls)
                .hasSize(3) // Includes European Master Data + 2 outbound connectors
                .anyMatch(url -> url.getName().equals("outbound-connector-connector1"))
                .anyMatch(url -> url.getName().equals("outbound-connector-connector2"))
                .anyMatch(url -> url.getName().equals("european-masterdata"));
    }

    @Test
    void testSwaggerUiConfig_withBothConnectors() {
        // Given
        String publicURL = "https://public.example.com";
        String managementURL = "https://management.example.com";
        SwaggerUiConfigProperties config = new SwaggerUiConfigProperties();
        OpenApiDocs openApiDocs = new OpenApiDocs();

        // When
        SwaggerUiConfigProperties result = openApiDocs.swaggerUiConfig(
                config,
                publicURL,
                managementURL,
                List.of("region1"),
                List.of("connector1")
        );

        // Then
        Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> urls = result.getUrls();
        assertThat(urls)
                .hasSize(3) // Includes all 3 APIs
                .anyMatch(url -> url.getName().equals("region-connector-region1"))
                .anyMatch(url -> url.getName().equals("outbound-connector-connector1"))
                .anyMatch(url -> url.getName().equals("european-masterdata"));
    }
}