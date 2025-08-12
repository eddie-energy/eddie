package energy.eddie.core.web;

import energy.eddie.core.services.MetadataService;
import energy.eddie.core.services.SupportedFeatureService;
import energy.eddie.spring.regionconnector.extensions.RegionConnectorSupportedFeatureExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ManagementController.class, properties = "eddie.management.server.urlprefix=management")
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
    private MetadataService metadataService;

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