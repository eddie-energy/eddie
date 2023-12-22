package energy.eddie.core.web;

import energy.eddie.api.v0.HealthState;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.core.services.HealthService;
import energy.eddie.core.services.MetadataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PermissionFacadeController.class)
class PermissionFacadeControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private MetadataService metadataService;
    @MockBean
    private HealthService healthService;

    @Test
    void regionConnectorsMetadata_returnsAllRegionConnectorsMetadata() throws Exception {
        when(metadataService.getRegionConnectorMetadata()).thenReturn(List.of(
                mock(RegionConnectorMetadata.class),
                mock(RegionConnectorMetadata.class)
        ));

        mockMvc.perform(get("/api/region-connectors-metadata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void regionConnectorsHealth_returnsAllRegionConnectorsHealth() throws Exception {
        var first = Map.of("first_one", HealthState.UP, "first_two", HealthState.DOWN);
        var second = Map.of("second_one", HealthState.UP);

        when(healthService.getRegionConnectorHealth()).thenReturn(Map.of("first", first, "second", second));

        mockMvc.perform(get("/api/region-connectors-health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.first", aMapWithSize(2)))
                .andExpect(jsonPath("$.second", aMapWithSize(1)));
    }
}
