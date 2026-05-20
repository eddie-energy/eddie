// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package org.springdoc.webmvc.ui;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SwaggerControllerTest {

    @Test
    void dynamicServerUrlCustomizer_shouldSetServerUrl_basedOnPublicUrl() {
        // Given
        int managementPort = 9090;
        String publicUrl = "http://localhost:8080";
        String managementUrl = "http://localhost:9090";
        SwaggerController swaggerController = new SwaggerController(
                null, null, null, managementPort, publicUrl, managementUrl);
        OpenApiCustomizer customizer = swaggerController.dynamicServerUrlCustomizer();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setLocalPort(8080);
        request.setServletPath("/api");

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        OpenAPI openApi = new OpenAPI();

        // When
        customizer.customise(openApi);

        // Then
        List<Server> servers = openApi.getServers();
        assertEquals(1, servers.size());
        assertEquals(publicUrl + "/api", servers.getFirst().getUrl());
    }

    @Test
    void dynamicServerUrlCustomizer_shouldSetServerUrl_basedOnManagementUrl() {
        // Given
        int managementPort = 9090;
        String publicUrl = "http://localhost:8080";
        String managementUrl = "http://localhost:9090";
        SwaggerController swaggerController = new SwaggerController(
                null, null, null, managementPort, publicUrl, managementUrl);
        OpenApiCustomizer customizer = swaggerController.dynamicServerUrlCustomizer();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setLocalPort(managementPort);
        request.setServletPath("/management");

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        OpenAPI openApi = new OpenAPI();

        // When
        customizer.customise(openApi);

        // Then
        List<Server> servers = openApi.getServers();
        assertEquals(1, servers.size());
        assertEquals(managementUrl + "/management", servers.getFirst().getUrl());
    }

    @Test
    void dynamicServerUrlCustomizer_shouldDoNothing_ifNoRequestAttributes() {
        // Given
        int managementPort = 9090;
        String publicUrl = "http://localhost:8080";
        String managementUrl = "http://localhost:9090";
        SwaggerController swaggerController = new SwaggerController(
                null, null, null, managementPort, publicUrl, managementUrl);
        OpenApiCustomizer customizer = swaggerController.dynamicServerUrlCustomizer();
        OpenAPI openApi = new OpenAPI();

        // When
        RequestContextHolder.resetRequestAttributes();
        customizer.customise(openApi);

        // Then
        List<Server> servers = openApi.getServers();
        assertNull(servers);
    }
}