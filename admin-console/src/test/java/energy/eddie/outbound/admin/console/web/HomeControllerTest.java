package energy.eddie.outbound.admin.console.web;

import energy.eddie.outbound.admin.console.data.StatusMessage;
import energy.eddie.outbound.admin.console.data.StatusMessageDTO;
import energy.eddie.outbound.admin.console.data.StatusMessageRepository;
import energy.eddie.outbound.admin.console.services.TerminationAdminConsoleConnector;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static energy.eddie.outbound.admin.console.config.AdminConsoleSecurityConfig.ADMIN_CONSOLE_BASE_URL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {HomeController.class}, properties = {
        "eddie.public.url=http://localhost:8080",
        "eddie.management.url=http://localhost:9090",
        "eddie.management.server.urlprefix=management",
        "outbound-connector.admin-console.login.enabled=true",
        "outbound-connector.admin-console.login.username=user",
        "outbound-connector.admin-console.login.encoded-password=$2a$10$qYTmwhGa3dd7Sl1CdXKKHOfmf0lNXL3L2k4CVhhm3CfY131hrcEyS"
})
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@WithMockUser
class HomeControllerTest {

    private final List<StatusMessage> statusMessages = List.of(
            new StatusMessage("testPermissionId",
                              "testRegionConnectorId",
                              "testDataNeedId",
                              "testCountry",
                              "testDso",
                              "2024-05-22T08:20:03+02:00",
                              "A06",
                              "ACCEPTED"),
            new StatusMessage("testPermissionId",
                              "testRegionConnectorId",
                              "testDataNeedId",
                              "testCountry",
                              "testDso",
                              "2024-05-22T08:20:03+02:00",
                              "A05",
                              "ACCEPTED")
    );

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private StatusMessageRepository statusMessageRepository;
    @MockitoBean
    private TerminationAdminConsoleConnector terminationConnector;
    @Value("${eddie.public.url}")
    private String publicUrl;
    @Value("${eddie.management.url}")
    private String managementUrl;
    @Value("${eddie.management.server.urlprefix}")
    private String managementUrlPrefix;

    @Test
    void testGetStatusMessages() throws Exception {
        // Given
        when(statusMessageRepository.findLatestStatusMessageForAllPermissions()).thenReturn(statusMessages);

        // When
        var json = mockMvc.perform(get("/statusMessages").accept(MediaType.APPLICATION_JSON))
                          .andReturn()
                          .getResponse()
                          .getContentAsString();

        List<StatusMessageDTO> result = objectMapper.readValue(json, new TypeReference<>() {});

        // Then
        assertEquals(statusMessages.size(), result.size());
    }

    @Test
    void testGetStatusMessagesByPermissionId() throws Exception {
        // Given
        when(statusMessageRepository.findByPermissionIdOrderByStartDateDescIdDesc("testPermissionId"))
                .thenReturn(statusMessages);

        // When
        var json = mockMvc.perform(get("/statusMessages/testPermissionId").accept(MediaType.APPLICATION_JSON))
                          .andExpect(status().isOk())
                          .andReturn()
                          .getResponse()
                          .getContentAsString();

        List<StatusMessageDTO> result = objectMapper.readValue(json, new TypeReference<>() {});

        // Then
        verify(statusMessageRepository, times(1)).findByPermissionIdOrderByStartDateDescIdDesc("testPermissionId");
        assertEquals(2, result.size());
        assertEquals("Available", result.get(0).cimStatus());
        assertEquals("Active", result.get(1).cimStatus());
    }

    @Test
    void testTerminatePermission() throws Exception {
        // Given
        StatusMessage testStatusMessage = new StatusMessage("testPermissionId",
                                                            "testCountry",
                                                            "testRegionConnectorId",
                                                            "testDataNeedId",
                                                            "testDso",
                                                            "2024-05-22T08:20:03+02:00",
                                                            "A05",
                                                            "ACCEPTED");
        when(statusMessageRepository.findByPermissionIdOrderByStartDateDescIdDesc("testPermissionId"))
                .thenReturn(List.of(testStatusMessage));

        // When
        mockMvc.perform(post("/terminate/testPermissionId").with(csrf()))
               .andExpect(status().isOk());

        // Then
        ArgumentCaptor<String> permissionIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> regionConnectorIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(terminationConnector, times(1)).terminate(permissionIdCaptor.capture(),
                                                         regionConnectorIdCaptor.capture());
    }

    @Test
    void testIndexIncludesModelAttributes() throws Exception {
        mockMvc.perform(get("/"))
               .andExpect(status().isOk())
               .andExpect(view().name("index"))
               .andExpect(model().attribute("eddiePublicUrl", publicUrl))
               .andExpect(model().attribute("eddieAdminConsoleUrl", managementUrl + ADMIN_CONSOLE_BASE_URL))
               .andExpect(model().attribute("eddieManagementUrl", managementUrl))
               .andExpect(model().attribute("eddieManagementUrlPrefix", managementUrlPrefix));
    }
}