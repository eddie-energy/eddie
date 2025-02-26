package energy.eddie.aiida.web;

import energy.eddie.aiida.dtos.installer.ChartMetadataDto;
import energy.eddie.aiida.dtos.installer.InstallerSetupDto;
import energy.eddie.aiida.dtos.installer.ReleaseInfoDto;
import energy.eddie.aiida.dtos.installer.VersionInfoDto;
import energy.eddie.aiida.errors.InstallerException;
import energy.eddie.aiida.services.InstallerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InstallerController.class)
@ExtendWith(MockitoExtension.class)
class InstallerControllerTest {
    private static final String BASE_PATH = "/installer";
    private static final String HEALTH_PATH = BASE_PATH + "/aiida";
    private static final String AIIDA_PATH = BASE_PATH + "/aiida";
    private static final String SERVICE_PATH = BASE_PATH + "/services/user";
    private static final String SAMPLE_CHART = "my-chart";
    private static final String SAMPLE_RELEASE = "my-release";
    private static final VersionInfoDto VERSION_INFO = new VersionInfoDto(
            "my-service-123",
            new ReleaseInfoDto(
                    ZonedDateTime.now(),
                    ZonedDateTime.now(),
                    null,
                    "Test release",
                    "DEPLOYED"
            ),
            new ChartMetadataDto(
                    "my-service",
                    "1.2.3",
                    "Test chart",
                    "3.2.1",
                    false
            ),
            new ChartMetadataDto(
                    "my-service",
                    "1.2.3",
                    "Test chart",
                    "3.2.1",
                    false
            )
    );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    @SuppressWarnings("unused")
    private InstallerService installerService;

    @BeforeEach
    void setUp() {
        reset(installerService);
    }

    @Test
    void health_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(get(HEALTH_PATH))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrlPattern("**/oauth2/authorization/keycloak"));
    }

    @Test
    @WithMockUser
    void health_withAuthentication_success() throws Exception {
        mockMvc.perform(get(HEALTH_PATH)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void aiidaVersion_success() throws Exception {
        when(installerService.aiidaVersion()).thenReturn(VERSION_INFO);

        mockMvc.perform(get(AIIDA_PATH))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.releaseName").value("my-service-123"));
    }

    @Test
    @WithMockUser
    void aiidaVersion_throwsException() throws Exception {
        when(installerService.aiidaVersion()).thenThrow(new InstallerException(HttpStatus.BAD_GATEWAY, "Error"));
        mockMvc.perform(get(AIIDA_PATH)).andExpect(status().isBadGateway());
    }

    @Test
    @WithMockUser
    void installOrUpgradeAiida_success() throws Exception {
        when(installerService.installOrUpgradeAiida()).thenReturn(VERSION_INFO);

        mockMvc.perform(post(AIIDA_PATH).with(csrf()))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.releaseName").value("my-service-123"));
    }

    @Test
    @WithMockUser
    void servicesVersions_success() throws Exception {
        when(installerService.servicesVersions()).thenReturn(List.of(VERSION_INFO));

        mockMvc.perform(get(SERVICE_PATH))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].releaseName").value("my-service-123"));
    }

    @Test
    @WithMockUser
    void installNewService_withoutConfig_success() throws Exception {
        when(installerService.installNewService(eq(SAMPLE_CHART), any())).thenReturn(VERSION_INFO);

        mockMvc.perform(post(SERVICE_PATH + "/" + SAMPLE_CHART).with(csrf()))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.releaseName").value("my-service-123"));
        verify(installerService).installNewService(SAMPLE_CHART, null);
    }

    @Test
    @WithMockUser
    void installNewService_withConfig_success() throws Exception {
        when(installerService.installNewService(eq(SAMPLE_CHART), any())).thenReturn(VERSION_INFO);
        mockMvc.perform(post(SERVICE_PATH + "/" + SAMPLE_CHART)
                                .content("{\"customValues\": [\"core.port=1234\", \"db.name=mydb\"]}")
                                .contentType("application/json")
                                .with(csrf())
               )
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.releaseName").value("my-service-123"));

        var argumentCaptor = ArgumentCaptor.forClass(InstallerSetupDto.class);
        verify(installerService).installNewService(eq(SAMPLE_CHART), argumentCaptor.capture());
        var customValues = argumentCaptor.getValue().customValues();
        assertEquals(List.of("core.port=1234", "db.name=mydb"), customValues);
    }

    @Test
    @WithMockUser
    void serviceVersion_success() throws Exception {
        when(installerService.serviceVersion(SAMPLE_CHART, SAMPLE_RELEASE)).thenReturn(VERSION_INFO);

        mockMvc.perform(get(SERVICE_PATH + "/" + SAMPLE_CHART + "/" + SAMPLE_RELEASE))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.releaseName").value("my-service-123"));
        verify(installerService).serviceVersion(SAMPLE_CHART, SAMPLE_RELEASE);
    }

    @Test
    @WithMockUser
    void installOrUpgradeService_success() throws Exception {
        when(installerService.installOrUpgradeService(SAMPLE_CHART, SAMPLE_RELEASE)).thenReturn(VERSION_INFO);

        mockMvc.perform(post(SERVICE_PATH + "/" + SAMPLE_CHART + "/" + SAMPLE_RELEASE).with(csrf()))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.releaseName").value("my-service-123"));
        verify(installerService).installOrUpgradeService(SAMPLE_CHART, SAMPLE_RELEASE);
    }

    @Test
    @WithMockUser
    void deleteService_success() throws Exception {
        mockMvc.perform(delete(SERVICE_PATH + "/" + SAMPLE_CHART + "/" + SAMPLE_RELEASE).with(csrf()))
               .andExpect(status().isOk());
        verify(installerService).deleteService(SAMPLE_CHART, SAMPLE_RELEASE);
    }
}
