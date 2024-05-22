package energy.eddie.core.web;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculation;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.api.v0.HealthState;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.core.services.DataNeedCalculationRouter;
import energy.eddie.core.services.HealthService;
import energy.eddie.core.services.MetadataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PermissionFacadeController.class)
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
class PermissionFacadeControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private MetadataService metadataService;
    @MockBean
    private HealthService healthService;
    @MockBean
    private DataNeedCalculationRouter router;

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

    @Test
    void regionConnectorsCalculateDataNeed_returnsDataNeedInformation() throws Exception {
        var now = LocalDate.now(ZoneOffset.UTC);
        when(router.calculateFor("at-eda", "9bd0668f-cc19-40a8-99db-dc2cb2802b17"))
                .thenReturn(new DataNeedCalculation(
                        true,
                        List.of(Granularity.PT15M),
                        new Timeframe(now, now),
                        new Timeframe(now, now)
                ));

        mockMvc.perform(get("/api/region-connectors/at-eda/data-needs/9bd0668f-cc19-40a8-99db-dc2cb2802b17"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.supportsDataNeed", is(true)))
               .andExpect(jsonPath("$.granularities", hasSize(1)));
    }

    @Test
    void regionConnectorsCalculateDataNeedForAll_returnsDataNeedInformation() throws Exception {
        var now = LocalDate.now(ZoneOffset.UTC);
        when(router.calculate("9bd0668f-cc19-40a8-99db-dc2cb2802b17"))
                .thenReturn(Map.of("at-eda",
                                   new DataNeedCalculation(
                                           true,
                                           List.of(Granularity.PT15M),
                                           new Timeframe(now, now),
                                           new Timeframe(now, now)
                                   )
                ));

        mockMvc.perform(get("/api/region-connectors/data-needs/9bd0668f-cc19-40a8-99db-dc2cb2802b17"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasKey("at-eda")));
    }
}
