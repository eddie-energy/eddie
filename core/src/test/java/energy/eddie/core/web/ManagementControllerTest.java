// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.web;

import energy.eddie.core.security.JwtIssuerFilter;
import energy.eddie.core.services.DataNeedRuleSetRouter;
import energy.eddie.core.services.SupportedFeatureService;
import energy.eddie.spring.regionconnector.extensions.RegionConnectorSupportedFeatureExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = ManagementController.class,
        properties = "eddie.management.server.urlprefix=management",
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtIssuerFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
class ManagementControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private SupportedFeatureService service;
    @MockitoBean
    private RegionConnectorSupportedFeatureExtension extension;
    @SuppressWarnings("unused")
    @MockitoBean
    private DataNeedRuleSetRouter router;

    @Test
    void testSupportedFeatures_returns200() throws Exception {
        // Given
        when(service.getSupportedFeatureExtensions()).thenReturn(List.of(extension));

        // When
        mockMvc.perform(get("/management/region-connectors/supported-features"))
               // Then
               .andExpect(status().isOk());
    }

    @Test
    void testSupportedDataNeeds_returns200() throws Exception {

        // When
        mockMvc.perform(get("/management/region-connectors/supported-data-needs"))
               // Then
               .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/"})
    void testIndex_returns404(String path) throws Exception {
        // Given
        // When
        mockMvc.perform(get(path))
               // Then
               .andExpect(status().isNotFound());
    }
}