package energy.eddie.core.web;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculation;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.core.application.information.ApplicationInformation;
import energy.eddie.core.services.ApplicationInformationService;
import energy.eddie.core.services.DataNeedCalculationRouter;
import energy.eddie.core.services.MetadataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
    @MockitoBean
    private MetadataService metadataService;
    @MockitoBean
    private DataNeedCalculationRouter router;
    @MockitoBean
    private ApplicationInformationService applicationInformationService;

    @Test
    void applicationInformation_returnsApplicationInformation() throws Exception {
        when(applicationInformationService.applicationInformation()).thenReturn(mock(ApplicationInformation.class));

        mockMvc.perform(get("/api/application-information")).andExpect(status().isOk());
    }

    @Test
    void regionConnectorsMetadata_returnsAllRegionConnectorsMetadata() throws Exception {
        when(metadataService.getRegionConnectorMetadata()).thenReturn(List.of(mock(RegionConnectorMetadata.class),
                                                                              mock(RegionConnectorMetadata.class)));

        mockMvc.perform(get("/api/region-connectors-metadata"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void regionConnectorsCalculateDataNeed_returnsDataNeedInformation() throws Exception {
        var now = LocalDate.now(ZoneOffset.UTC);
        when(router.calculateFor("at-eda", "9bd0668f-cc19-40a8-99db-dc2cb2802b17")).thenReturn(new DataNeedCalculation(
                true,
                List.of(Granularity.PT15M),
                new Timeframe(now, now),
                new Timeframe(now, now)));

        mockMvc.perform(get("/api/region-connectors/at-eda/data-needs/9bd0668f-cc19-40a8-99db-dc2cb2802b17"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.supportsDataNeed", is(true)))
               .andExpect(jsonPath("$.granularities", hasSize(1)));
    }

    @Test
    void regionConnectorsCalculateDataNeedForAll_returnsDataNeedInformation() throws Exception {
        var now = LocalDate.now(ZoneOffset.UTC);
        when(router.calculate("9bd0668f-cc19-40a8-99db-dc2cb2802b17")).thenReturn(Map.of("at-eda",
                                                                                         new DataNeedCalculation(true,
                                                                                                                 List.of(Granularity.PT15M),
                                                                                                                 new Timeframe(
                                                                                                                         now,
                                                                                                                         now),
                                                                                                                 new Timeframe(
                                                                                                                         now,
                                                                                                                         now))));

        mockMvc.perform(get("/api/region-connectors/data-needs/9bd0668f-cc19-40a8-99db-dc2cb2802b17"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasKey("at-eda")));
    }
}
