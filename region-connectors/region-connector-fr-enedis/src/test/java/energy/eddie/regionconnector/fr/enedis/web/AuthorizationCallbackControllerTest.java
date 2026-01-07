package energy.eddie.regionconnector.fr.enedis.web;

import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.fr.enedis.CimTestConfiguration;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionEventRepository;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import energy.eddie.regionconnector.fr.enedis.services.PermissionRequestService;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthorizationCallbackController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(CimTestConfiguration.class)
class AuthorizationCallbackControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PermissionRequestService permissionRequestService;
    @MockitoBean
    @SuppressWarnings("unused")
    private FrPermissionRequestRepository unusedRepository;
    @MockitoBean
    @SuppressWarnings("unused")
    private FrPermissionEventRepository permissionEventRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private DataNeedsService dataNeedsService;

    @Test
    void authorizationCallback_withParams_returnsAttributes() throws Exception {
        // Given
        doNothing().when(permissionRequestService).authorizePermissionRequest(anyString(), any());

        // When
        mockMvc.perform(
                       MockMvcRequestBuilders.get("/authorization-callback")
                                             .param("state", "state")
                                             .param("usage_point_id", "upid1;upid2;upid3")
               )
               // Then
               .andExpect(status().isOk())
               .andExpect(model().attribute("status", "OK"))
               .andExpect(model().attribute("usagePointIds", "upid1, upid2, upid3"));
    }

    @Test
    void authorizationCallback_noUsagePointId_returnsDenied() throws Exception {
        // Given
        doNothing().when(permissionRequestService).authorizePermissionRequest(anyString(), any());

        // When
        mockMvc.perform(
                       MockMvcRequestBuilders.get("/authorization-callback")
                                             .param("state", "state")
               )
               // Then
               .andExpect(status().isOk())
               .andExpect(model().attribute("status", "DENIED"));
    }

    @Test
    void authorizationCallback_noState_returnsDenied() throws Exception {
        // Given
        doNothing().when(permissionRequestService).authorizePermissionRequest(anyString(), any());

        // When
        mockMvc.perform(
                       MockMvcRequestBuilders.get("/authorization-callback")
                                             .param("usage_point_id", "upid1;upid2;upid3")
               )
               // Then
               .andExpect(status().isOk())
               .andExpect(model().attribute("status", "DENIED"));
    }

    @Test
    void authorizationCallback_withInvalidPermission_returnsError() throws Exception {
        // Given
        doThrow(new PermissionNotFoundException("")).when(permissionRequestService)
                                                    .authorizePermissionRequest(anyString(), any());

        // When
        mockMvc.perform(
                       MockMvcRequestBuilders.get("/authorization-callback")
                                             .param("state", "state")
                                             .param("usage_point_id", "invalid")
               )
               // Then
               .andExpect(status().isOk())
               .andExpect(model().attribute("status", "ERROR"));
    }
}
