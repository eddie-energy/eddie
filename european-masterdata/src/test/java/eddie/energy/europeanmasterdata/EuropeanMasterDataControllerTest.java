package eddie.energy.europeanmasterdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.master.data.MasterDataCollection;
import energy.eddie.api.agnostic.master.data.MeteredDataAdministrator;
import energy.eddie.api.agnostic.master.data.PermissionAdministrator;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;
import java.util.Optional;

@WebMvcTest(EuropeanMasterDataController.class)
class EuropeanMasterDataControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private MasterDataCollection masterDataService;

    @Test
    void getPermissionAdministrators() throws Exception {
        final var permissionAdministrators = List.of(new PermissionAdministrator("country",
                                                                                 "company",
                                                                                 "name",
                                                                                 "company-id",
                                                                                 "jumpOffUrl",
                                                                                 "regionConnector"));
        BDDMockito.given(this.masterDataService.getPermissionAdministrators())
                  .willReturn(permissionAdministrators);
        mvc.perform(MockMvcRequestBuilders.get("/api/permission-administrators").accept(MediaType.APPLICATION_JSON))
           .andExpect(MockMvcResultMatchers.status().isOk())
           .andExpect(MockMvcResultMatchers
                              .content()
                              .json(objectMapper.writeValueAsString(permissionAdministrators),
                                    JsonCompareMode.STRICT));
    }

    @Test
    void getPermissionAdministrator() throws Exception {
        final var permissionAdministrator = new PermissionAdministrator("country",
                                                                        "company",
                                                                        "name",
                                                                        "company-id",
                                                                        "jumpOffUrl",
                                                                        "regionConnector");
        BDDMockito.given(this.masterDataService.getPermissionAdministrator("company-id"))
                  .willReturn(Optional.of(permissionAdministrator));
        mvc.perform(MockMvcRequestBuilders.get("/api/permission-administrators/company-id")
                                          .accept(MediaType.APPLICATION_JSON))
           .andExpect(MockMvcResultMatchers.status().isOk())
           .andExpect(MockMvcResultMatchers
                              .content()
                              .json(objectMapper.writeValueAsString(permissionAdministrator),
                                    JsonCompareMode.STRICT));
    }

    @Test
    void getPermissionAdministrator_notFound() throws Exception {
        BDDMockito.given(this.masterDataService.getPermissionAdministrator("nonexistent-id"))
                  .willReturn(Optional.empty());
        mvc.perform(MockMvcRequestBuilders.get("/api/permission-administrators/nonexistent-id")
                                          .accept(MediaType.APPLICATION_JSON))
           .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void getMeteredDataAdministrators() throws Exception {
        final var meteredDataAdministrators = List.of(new MeteredDataAdministrator("country",
                                                                                   "company",
                                                                                   "company-id",
                                                                                   "websiteUrl",
                                                                                   "officialContact",
                                                                                   "permissionAdministrator"));
        BDDMockito.given(this.masterDataService.getMeteredDataAdministrators())
                  .willReturn(meteredDataAdministrators);
        mvc.perform(MockMvcRequestBuilders.get("/api/metered-data-administrators").accept(MediaType.APPLICATION_JSON))
           .andExpect(MockMvcResultMatchers.status().isOk())
           .andExpect(MockMvcResultMatchers
                              .content()
                              .json(objectMapper.writeValueAsString(meteredDataAdministrators),
                                    JsonCompareMode.STRICT));
    }

    @Test
    void getMeteredDataAdministrator() throws Exception {
        final var meteredDataAdministrator = new MeteredDataAdministrator("country",
                                                                          "company",
                                                                          "company-id",
                                                                          "websiteUrl",
                                                                          "officialContact",
                                                                          "permissionAdministrator");
        BDDMockito.given(this.masterDataService.getMeteredDataAdministrator("company-id"))
                  .willReturn(Optional.of(meteredDataAdministrator));
        mvc.perform(MockMvcRequestBuilders.get("/api/metered-data-administrators/company-id")
                                          .accept(MediaType.APPLICATION_JSON))
           .andExpect(MockMvcResultMatchers.status().isOk())
           .andExpect(MockMvcResultMatchers
                              .content()
                              .json(objectMapper.writeValueAsString(meteredDataAdministrator),
                                    JsonCompareMode.STRICT));
    }

    @Test
    void getMeteredDataAdministrator_notFound() throws Exception {
        BDDMockito.given(this.masterDataService.getMeteredDataAdministrator("nonexistent-id"))
                  .willReturn(Optional.empty());
        mvc.perform(MockMvcRequestBuilders.get("/api/metered-data-administrators/nonexistent-id")
                                          .accept(MediaType.APPLICATION_JSON))
           .andExpect(MockMvcResultMatchers.status().isNotFound());
    }
}
