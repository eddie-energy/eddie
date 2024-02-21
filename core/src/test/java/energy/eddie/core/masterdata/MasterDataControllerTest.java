package energy.eddie.core.masterdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MasterDataController.class)
class MasterDataControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private MasterDataService masterDataService;

    @Test
    void getPermissionAdministrators() throws Exception {
        final var permissionAdministrators = List.of(new PermissionAdministrator("country", "company", "company-id", "jumpOffUrl", "regionConnector"));
        given(this.masterDataService.getPermissionAdministrators())
                .willReturn(permissionAdministrators);
        mvc.perform(get("/api/permission-administrators").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(permissionAdministrators), true));
    }

    @Test
    void getPermissionAdministrator() throws Exception {
        final var permissionAdministrator = new PermissionAdministrator("country", "company", "company-id", "jumpOffUrl", "regionConnector");
        given(this.masterDataService.getPermissionAdministrator("company-id"))
                .willReturn(Optional.of(permissionAdministrator));
        mvc.perform(get("/api/permission-administrators/company-id").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(permissionAdministrator), true));
    }

    @Test
    void getPermissionAdministrator_notFound() throws Exception {
        given(this.masterDataService.getPermissionAdministrator("nonexistent-id"))
                .willReturn(Optional.empty());
        mvc.perform(get("/api/permission-administrators/nonexistent-id").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMeteredDataAdministrators() throws Exception {
        final var meteredDataAdministrators = List.of(new MeteredDataAdministrator("country", "company", "company-id", "websiteUrl", "officialContact"));
        given(this.masterDataService.getMeteredDataAdministrators())
                .willReturn(meteredDataAdministrators);
        mvc.perform(get("/api/metered-data-administrators").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(meteredDataAdministrators), true));
    }

    @Test
    void getMeteredDataAdministrator() throws Exception {
        final var meteredDataAdministrator = new MeteredDataAdministrator("country", "company", "company-id", "websiteUrl", "officialContact");
        given(this.masterDataService.getMeteredDataAdministrator("company-id"))
                .willReturn(Optional.of(meteredDataAdministrator));
        mvc.perform(get("/api/metered-data-administrators/company-id").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(meteredDataAdministrator), true));
    }

    @Test
    void getMeteredDataAdministrator_notFound() throws Exception {
        given(this.masterDataService.getMeteredDataAdministrator("nonexistent-id"))
                .willReturn(Optional.empty());
        mvc.perform(get("/api/metered-data-administrators/nonexistent-id").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
